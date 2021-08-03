package tech.blockchainers.gnosissafeclient.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;
import tech.blockchainers.GnosisSafe;
import tech.blockchainers.SampleToken;
import tech.blockchainers.gnosissafeclient.config.CredentialHolder;
import tech.blockchainers.gnosissafeclient.rest.dto.*;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
public class GnosisSafeService {

    @Value("${key.salt}")
    private String salt;

    private final Web3j web3j;
    private final CredentialHolder credentialHolder;

    public GnosisSafeService(Web3j web3j, CredentialHolder credentialHolder) {
        this.web3j = web3j;
        this.credentialHolder = credentialHolder;
    }

    @GetMapping("/createKey")
    public KeyPairDto createKey(@RequestParam byte[] seed) {
        byte[] combinedSeed = createSeed(seed);
        Credentials credentials = createCredentials(combinedSeed);
        KeyPairDto keyPairDto = new KeyPairDto();
        keyPairDto.setPrivateKeyHex(Numeric.toHexStringWithPrefix(credentials.getEcKeyPair().getPrivateKey()));
        keyPairDto.setPublicKeyHex(Numeric.toHexStringWithPrefix(credentials.getEcKeyPair().getPublicKey()));
        keyPairDto.setAddressHex(credentials.getAddress());
        return keyPairDto;
    }

    private Credentials createCredentials(byte[] combinedSeed) {
        Bip32ECKeyPair keyPair = Bip32ECKeyPair.generateKeyPair(combinedSeed);
        return Credentials.create(keyPair);
    }

    private byte[] createSeed(byte[] seed) {
        byte[] saltB = salt.getBytes(StandardCharsets.UTF_8);
        byte[] combinedSeed = new byte[saltB.length + seed.length];
        System.arraycopy(saltB, 0, combinedSeed, 0, saltB.length);
        System.arraycopy(seed, 0, combinedSeed, saltB.length, seed.length);
        return combinedSeed;
    }

    @PatchMapping("/setupSafe")
    public GnosisSafeDto setupGnosisSafe(@RequestBody GnosisSafeSetupDto gnosisSafeSetup) throws Exception {
        GnosisSafe gnosisSafeFacade = GnosisSafe.load(gnosisSafeSetup.getSafeAddress(), web3j, credentialHolder.deriveChildKeyPair(0), new DefaultGasProvider());
        gnosisSafeFacade.setup(
                Arrays.asList(gnosisSafeSetup.getOwners()), BigInteger.valueOf(gnosisSafeSetup.getThreshold()), "0x0000000000000000000000000000000000000000",
                new byte[0], "0x0000000000000000000000000000000000000000", "0x0000000000000000000000000000000000000000", BigInteger.ZERO, "0x0000000000000000000000000000000000000000").send();

        return GnosisSafeDto.builder().address(gnosisSafeSetup.getSafeAddress()).owners(Arrays.asList(gnosisSafeSetup.getOwners())).threshold(gnosisSafeSetup.getThreshold()).build();
    }

    @GetMapping("/signTransaction")
    public SignatureDto signTransaction(@RequestParam byte[] seed, @RequestParam String gnosisSafeAddress, @RequestParam String tokenAddress, @RequestParam String to, @RequestParam Integer value) throws Exception {
        byte[] combinedSeed = createSeed(seed);
        Credentials credentials = createCredentials(combinedSeed);

        SampleToken sampleToken = SampleToken.load(tokenAddress, web3j, credentials, new DefaultGasProvider());
        String encodedFunc = sampleToken.transfer(to, BigInteger.valueOf(value)).encodeFunctionCall();
        log.info("Encoded Func {}", encodedFunc);

        GnosisSafe gnosisSafeFacade = GnosisSafe.load(gnosisSafeAddress, web3j, credentials, new DefaultGasProvider());

        BigInteger nounce = gnosisSafeFacade.nonce().send();

        byte[] hash = gnosisSafeFacade.getTransactionHash(sampleToken.getContractAddress(),
                BigInteger.ZERO, Numeric.hexStringToByteArray(encodedFunc), BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO,
                "0x0000000000000000000000000000000000000000", "0x0000000000000000000000000000000000000000", nounce).send();

        byte[] retval = new byte[65];
        Sign.SignatureData signature = Sign.signMessage(hash, credentials.getEcKeyPair(), false);
        System.arraycopy(signature.getR(), 0, retval, 0, 32);
        System.arraycopy(signature.getS(), 0, retval, 32, 32);
        System.arraycopy(signature.getV(), 0, retval, 64, 1);
        log.info("Signatures {}", Numeric.toHexString(retval));

        SignatureDto.SignatureDtoBuilder signatureDtoBuilder = SignatureDto.builder();
        signatureDtoBuilder.dataHash(Numeric.toHexString(hash));
        signatureDtoBuilder.signature(Numeric.toHexString(retval));
        return signatureDtoBuilder.build();
    }

    @GetMapping("/sendTransaction")
    public SentTransactionDto sendTransaction(@RequestParam String gnosisSafeAddress, @RequestParam String tokenAddress, @RequestParam String to, @RequestParam Integer value, @RequestParam String[] addressAndSignature) throws Exception {
        GnosisSafe gnosisSafeFacade = GnosisSafe.load(gnosisSafeAddress, web3j, credentialHolder.deriveChildKeyPair(0), new DefaultGasProvider());

        SampleToken sampleToken = SampleToken.load(tokenAddress, web3j, credentialHolder.deriveChildKeyPair(0), new DefaultGasProvider());

        String encodedFunc = sampleToken.transfer(to, BigInteger.valueOf(value)).encodeFunctionCall();
        log.info("Encoded Func {}", encodedFunc);

        List<AddressSignatureDto> addressSignatureDtos = mapAddressAndSignatures(addressAndSignature);
        addressSignatureDtos.sort(Comparator.comparing(AddressSignatureDto::getAddress));

        int size = 0;
        List<byte[]> sigs = new ArrayList<>();
        for (AddressSignatureDto addressSignatureDto : addressSignatureDtos) {
            byte[] sigB = Numeric.hexStringToByteArray(addressSignatureDto.getSignature());
            size += sigB.length;
            sigs.add(sigB);
        }
        byte[] allSigs = new byte[size];
        int offset = 0;
        for (byte[] sigb : sigs) {
            System.arraycopy(sigb, 0, allSigs, offset, sigb.length);
            offset += sigb.length;
        }

        TransactionReceipt finalHash = gnosisSafeFacade.execTransaction(sampleToken.getContractAddress(),
                BigInteger.ZERO, Numeric.hexStringToByteArray(encodedFunc), BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO,
                "0x0000000000000000000000000000000000000000", "0x0000000000000000000000000000000000000000", allSigs).send();

        SentTransactionDto.SentTransactionDtoBuilder sentTransactionDtoBuilder = SentTransactionDto.builder();
        return sentTransactionDtoBuilder.signatures(Numeric.toHexString(allSigs)).transactionHash(finalHash.getTransactionHash()).build();
    }

    private List<AddressSignatureDto> mapAddressAndSignatures(String[] addressAndSignatures) {
        List<AddressSignatureDto> addressSignatureDtos = new ArrayList<>();
        for (String addressAndSignature : addressAndSignatures) {
            String[] addressSignature = addressAndSignature.split(";");
            AddressSignatureDto addressSignatureDto = AddressSignatureDto.builder().address(addressSignature[0]).signature(addressSignature[1]).build();
            addressSignatureDtos.add(addressSignatureDto);
        }
        return addressSignatureDtos;
    }


    @PostMapping("/deploySafe")
    public GnosisSafeDto createGnosisSafe() throws Exception {
        GnosisSafe gnosisSafe = GnosisSafe.deploy(web3j, credentialHolder.deriveChildKeyPair(0), new DefaultGasProvider()).send();
        log.info("Deploy Gnosis Safe Contract to {}", gnosisSafe.getContractAddress());
        GnosisSafeDto.GnosisSafeDtoBuilder gnosisSafeDtoBuilder = GnosisSafeDto.builder();
        gnosisSafeDtoBuilder.address(gnosisSafe.getContractAddress());
        return gnosisSafeDtoBuilder.build();
    }

    @PostMapping("/deploySampleToken")
    public SampleTokenDto createSampleToken(@RequestParam String gnosisSafeAddress, @RequestParam Long value) throws Exception {
        SampleToken sampleToken = SampleToken.deploy(web3j, credentialHolder.deriveChildKeyPair(0), new DefaultGasProvider(), "TKN", "TKN").send();
        log.info("Deploy Gnosis Safe Contract to {}", sampleToken.getContractAddress());
        sampleToken.mintToken(gnosisSafeAddress, BigInteger.valueOf(value)).send();
        SampleTokenDto.SampleTokenDtoBuilder sampleTokenDtoBuilder = SampleTokenDto.builder();
        return sampleTokenDtoBuilder.address(sampleToken.getContractAddress()).build();
    }
}
