#fabric-network-builder-local

Builder yml for fabric network, which can be loaded by `org.hyperledger.fabric.sdk.NetworkConfig`

How to use: 

1. Copy fabric network `crypto-config` to `src/main/resources/crypto-config`.

2. Config organiztion, peer and IP address in `NetworkGenerator`.

3. Run the `NetworkGenerator`, and you'll get the fabric network config file from `src/main/resources/network`.

4. Run `NetworkTest` to test it.


### 中文使用

1. 将fabric网络的`证书文件`拷贝到`resource`目录下。
2. 在`NetworkGenerator`中配置`组织`、`peer`的IP地址。
3. 运行`NetworkGenerator`，生成fabric的网络连接文件`connection-*.yml`。
4. 通过`NetworkTest`测试。

