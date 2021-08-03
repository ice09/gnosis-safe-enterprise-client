package tech.blockchainers.gnosissafeclient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.DefaultGasProvider;
import tech.blockchainers.SampleToken;
import tech.blockchainers.gnosissafeclient.config.CredentialHolder;

import java.math.BigInteger;

@SpringBootTest
class GnosisSafeClientApplicationTests {

	@Autowired
	private Web3j web3j;

	@Autowired
	private CredentialHolder credentialHolder;

	@Test
	void contextLoads() throws Exception {
		Credentials userFrom = credentialHolder.deriveChildKeyPair(0);
		Credentials userTo = credentialHolder.deriveChildKeyPair(1);
		Credentials tokenDeployer = credentialHolder.deriveChildKeyPair(2);
		SampleToken sampleToken = deployToken(tokenDeployer);
		sampleToken.mintToken(userFrom.getAddress(), BigInteger.valueOf(50)).send();

		SampleToken sampleTokenLoadedFromFrom = SampleToken.load(sampleToken.getContractAddress(), web3j, userFrom, new DefaultGasProvider());
		sampleTokenLoadedFromFrom.approve(userTo.getAddress(), BigInteger.valueOf(50)).send();

		SampleToken sampleTokenLoadedFromTo = SampleToken.load(sampleToken.getContractAddress(), web3j, userTo, new DefaultGasProvider());
		sampleTokenLoadedFromTo.transferFrom(userFrom.getAddress(), userTo.getAddress(), BigInteger.TEN).send();
	}

	private SampleToken deployToken(Credentials deployer) throws Exception {
        return SampleToken.deploy(web3j, deployer, new DefaultGasProvider(), "Token", "TKN").send();
    }

}
