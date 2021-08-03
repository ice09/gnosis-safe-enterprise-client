package tech.blockchainers.gnosissafeclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;
import tech.blockchainers.GnosisSafe;
import tech.blockchainers.SampleToken;
import tech.blockchainers.gnosissafeclient.config.CredentialHolder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
@Slf4j
public class GnosisSafeClientApplication implements CommandLineRunner {

	@Autowired
	private ConfigurableApplicationContext ctx;

	@Autowired
	private CredentialHolder credentialHolder;

	@Autowired
	private Web3j web3j;

	public static void main(String[] args) {
		SpringApplication.run(GnosisSafeClientApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		GnosisSafe gnosisSafe = createGnosisSafe();
		SampleToken sampleToken = createSampleToken();
		log.info("GNOSIS SAFE ADDRESS: " + gnosisSafe.getContractAddress());
		log.info("SAMPLE TOKEN ADDRESS: " + sampleToken.getContractAddress());

		// Mint Token to account 0
		sampleToken.mintToken(credentialHolder.deriveChildKeyPair(0).getAddress(), BigInteger.valueOf(100)).send();

		// Transfer 10 Token to Gnosis Safe address
		sampleToken.transfer(gnosisSafe.getContractAddress(), BigInteger.TEN).send();

		//runCompleteITTest();
	}

	private void runCompleteITTest() throws Exception {
		GnosisSafe gnosisSafe = createGnosisSafe();
		SampleToken sampleToken = createSampleToken();
		log.info(gnosisSafe.getContractAddress());

		// Mint Token to account 0
		sampleToken.mintToken(credentialHolder.deriveChildKeyPair(0).getAddress(), BigInteger.valueOf(100)).send();

		// Transfer 10 Token to Gnosis Safe address
		sampleToken.transfer(gnosisSafe.getContractAddress(), BigInteger.TEN).send();

		// Create encoded Functional Call: Transfer 10 Token to account 2
		String encodedFunc = sampleToken.transfer(credentialHolder.deriveChildKeyPair(2).getAddress(), BigInteger.TEN).encodeFunctionCall();
		log.info("Encoded Func {}", encodedFunc);
		byte[] hash = gnosisSafe.getTransactionHash(sampleToken.getContractAddress(),
				BigInteger.ZERO, Numeric.hexStringToByteArray(encodedFunc), BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO,
				"0x0000000000000000000000000000000000000000", "0x0000000000000000000000000000000000000000", BigInteger.ZERO).send();

		log.info("Hash {} ", Numeric.toHexString(hash));

		int index1 = 10;
		int index2 = 11;
		int index3 = 12;
		List<Credentials> signerCreds = new ArrayList<>();
		signerCreds.add(credentialHolder.deriveChildKeyPair(index1));
		signerCreds.add(credentialHolder.deriveChildKeyPair(index2));
		signerCreds.add(credentialHolder.deriveChildKeyPair(index3));
		signerCreds.sort(Comparator.comparing(Credentials::getAddress));

		byte[] retval = new byte[65 * signerCreds.size()];
		int index = 0;
		for (Credentials signerCred : signerCreds) {
			Sign.SignatureData signature = Sign.signMessage(hash, signerCred.getEcKeyPair(), false);
			System.arraycopy(signature.getR(), 0, retval, 0 + (65 * index), 32);
			System.arraycopy(signature.getS(), 0, retval, 32 + (65 * index), 32);
			System.arraycopy(signature.getV(), 0, retval, 64 + (65 * index), 1);
			log.info("Signatures {}", Numeric.toHexString(retval));
			index++;
		}

		// Setup Gnosis Safe index 10,11,12 as owners and threshold of 3, ie. all signatures necessary
		gnosisSafe.setup(
				signerCreds.stream().map(Credentials::getAddress).collect(Collectors.toList()), BigInteger.valueOf(3), "0x0000000000000000000000000000000000000000",
				new byte[0], "0x0000000000000000000000000000000000000000", "0x0000000000000000000000000000000000000000", BigInteger.ZERO, "0x0000000000000000000000000000000000000000").send();

		TransactionReceipt finalHash = gnosisSafe.execTransaction(sampleToken.getContractAddress(),
				BigInteger.ZERO, Numeric.hexStringToByteArray(encodedFunc), BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO,
				"0x0000000000000000000000000000000000000000", "0x0000000000000000000000000000000000000000", retval).send();
		log.info("yay, got final trxhash {}", finalHash);
		// SpringApplication.exit(ctx, () -> 0);
	}

	public GnosisSafe createGnosisSafe() throws Exception {
		GnosisSafe gnosisSafe = GnosisSafe.deploy(web3j, credentialHolder.deriveChildKeyPair(0), new DefaultGasProvider()).send();
		log.info("Deploy Gnosis Safe Contract to {}", gnosisSafe.getContractAddress());
		return gnosisSafe;
	}

	public SampleToken createSampleToken() throws Exception {
		SampleToken sampleToken = SampleToken.deploy(web3j, credentialHolder.deriveChildKeyPair(0), new DefaultGasProvider(), "TKN", "TKN").send();
		log.info("Deploy Gnosis Safe Contract to {}", sampleToken.getContractAddress());
		return sampleToken;
	}
}
