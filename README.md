## Gnosis-Safe Client Enterprise Version

Spring Boot Web3j/EVM interface for the Gnosis Safe contracts.  
The Spring Boot web application provides Enterprise-grade User-, Rights- and Rolesmanagement, Authentication & Access Control.  
All Gnosis-Safe relevant actions are delegated to the Smart Contract by web3j.

![flow](https://drive.google.com/uc?export=view&id=1_Yltd2CAQWhsyABwvxKc9U5IzxjS9MJp)

### Prerequisites

* Java 11+
* Maven 3+

### Build

* Build with `mvnw`

### Setup
1. Install [Hardhat](https://hardhat.org/getting-started/#quick-start)
2. Run `npx hardhat node` with mnemonic *test test test test test test test test test test test junk* (in `.env`)

### Run

* Start with `mvnw spring-boot:run`

### Gnosis Safe Client Interface API

For the complete API see ... on the locally running application.

#### Setup

_Note: All steps described here are included in the REST test file, see 'Testing the Gnosis Safe API with VSCode'_  

The setup steps are not necessary for the core functionality.  
Gnosis Safe and Token with allowance for the Safe can also be deployed separately.

##### Deploy Safe

    POST http://localhost:8888/api/deploySafe HTTP/1.1
    content-type: application/json

    @safeAddress = {{safeDto.response.body.address}}

##### Deploy Sample Token with allowance for Safe

    POST http://localhost:8888/api/deploySampleToken?gnosisSafeAddress={{safeAddress}}&value=100 HTTP/1.1
    content-type: application/json

    @tokenAddress = {{tokenDto.response.body.address}}

#### Utility: Create Owner Address from Seed
    
    GET http://localhost:8888/api/createKey?seed=seedX HTTP/1.1

    @ownerAddressX = {{keyDto.response.body.addressHex}}

#### Setup Safe for owner 1-3 (with 2/3 threshold)

    PATCH http://localhost:8888/api/setupSafe HTTP/1.1
    content-type: application/json

    {
       "safeAddress": "{{safeAddress}}",
       "owners": [
           "{{ownerAddress1}}",
           "{{ownerAddress2}}",
           "{{ownerAddress3}}"
       ],
       "threshold": 2
    }

#### Sign Transaction with key1 ("seed1")

Sign a transfer of 10 Tokens.

    GET http://localhost:8888/api/signTransaction?seed=seed1&gnosisSafeAddress={{safeAddress}}&tokenAddress={{tokenAddress}}&to=0x0000000000000000000000000000000000000001&value=10

    @signature1 = {{signatureDto1.response.body.signature}}

#### Send Transaction key1,sig1,key2,sig2

    GET http://localhost:8888/api/sendTransaction?gnosisSafeAddress={{safeAddress}}&tokenAddress={{tokenAddress}}&to=0x0000000000000000000000000000000000000001&value=10&addressAndSignature={{ownerAddress1}};{{signature1}},{{ownerAddress2}};{{signature2}}

### Testing the Gnosis Safe API with VS Code

All the steps above are included in the `safe-script.http` file in root directory.  
To execute the tests, [VS Code](https://code.visualstudio.com/) must be installed with the [REST plugin](https://marketplace.visualstudio.com/items?itemName=humao.rest-client).

### Additional Information

#### Flatten & Compile Gnosis-Safe
* Clone [gnosis-safe project](https://github.com/gnosis/safe-contracts) to folder `${gnosis.git}/contracts`
* Install [remixd-server](https://github.com/ethereum/remix-project/tree/master/libs/remixd)
* Start remixd-server with folder `${gnosis.git}/contracts` and URL http://remix.ethereum.org/
* Open [remix-ide](http://remix.ethereum.org/)
   * Install Flattener in Remix-IDE, Flatten `GnosisSafe.sol`
   * Connect to localhost
   * Load `GnosisSafe.sol`
   * Store flattened Solidity file under `/src/main/resources/solidity`
   * Build with `mvn clean package`