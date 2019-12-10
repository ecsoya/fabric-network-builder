#fabric-network-builder-bbe

Builder yml for fabric network, which can be loaded by `org.hyperledger.fabric.sdk.NetworkConfig`

How to use: 

1. Download fabric network `crypto 网络证书` from BBE, and unzip to `src/main/resources/bbe`.

2. Config organiztion, peer and IP address in `BbeNetworkGenerator`.

3. Run the `BbeNetworkGenerator`, and you'll get the fabric network config file from `src/main/resources/network`.

4. Run `BbeNetworkTest` to test it.