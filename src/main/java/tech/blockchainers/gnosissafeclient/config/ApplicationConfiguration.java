package tech.blockchainers.gnosissafeclient.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.MnemonicUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
@Slf4j
public class ApplicationConfiguration {

    @Value("${ethereum.rpc.url}")
    private String ethereumRpcUrl;

    @Value("${web3.mnemonic}")
    private String mnemonic;

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(ethereumRpcUrl));
    }

    @Bean
    public Bip32ECKeyPair createMasterKeyPair() {
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, "");
        return Bip32ECKeyPair.generateKeyPair(seed);
    }

    @Bean
    public CredentialHolder createCredentials() {
        return new CredentialHolder(createMasterKeyPair());
    }

}