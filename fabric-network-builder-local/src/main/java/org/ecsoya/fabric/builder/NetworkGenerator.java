package org.ecsoya.fabric.builder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.hyperledger.fabric.sdk.NetworkConfig;
import org.yaml.snakeyaml.Yaml;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * 
 * Build a yml file of fabric network, which can be loaded by {@link NetworkConfig}.
 * 
 * @author ecsoya
 *
 */
public class NetworkGenerator {

	public static void main(String[] args) {
		String domain = "example.com";
		String[] clients = { "org1", "org2" };

		for (int i = 0; i < clients.length; i++) {
			try {
				File root = new File("src/main/resources");

				JsonObject json = new NetworkBuilder(domain).
						// Name of fabric network.
						name("example-fabric-network").
						// Root organization name
						clientOrg(clients[i])
						// Order org
						.ordererOrg("orderer")
						// All orderers: order1, order2...
						.orderers("orderer")
						// All orgs: org1, org2...
						.peerOrgs(clients)
						// All peers: peer0, peer1...
						.peers("peer0", "peer1")
						// Channel name
						.channels("common")
						
						// Root Directory of crypto files.
						.root(new File(root, "crypto-config"))
						
						// IP address binding for peer of orgs, '*' means all peers of a org. 
						.url("org1", "*", "192.168.0.1").url("org2", "*", "192.168.0.1")
						.url("orderer", "*", "192.168.0.1")
						
						// Port binding for peer of orgs, '*' means all peers of a org. 
						.port("org1", "peer0", 7051)
						.port("org1", "peer1", 8051)
						.port("org2", "peer0", 9051)
						.port("org2", "peer1", 10051)
						
						// Build to JSON.
						.build();

				Gson gson = new Gson();
				String value = gson.toJson(json);

				// Write the network config file to yml.
				Yaml yaml = new Yaml();
				Object map = yaml.load(value);
				String yvalue = yaml.dump(map);

				// Output
				File file = new File(root, "network/connection-" + clients[i] + ".yml");
				if (!file.exists()) {
					Files.write(file.toPath(), yvalue.getBytes(), StandardOpenOption.CREATE_NEW,
							StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
				} else {
					Files.write(file.toPath(), yvalue.getBytes(), StandardOpenOption.TRUNCATE_EXISTING,
							StandardOpenOption.WRITE);
				}

			} catch (NetworkBuilderException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
