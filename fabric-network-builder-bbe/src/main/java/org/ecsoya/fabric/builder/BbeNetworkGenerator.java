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
 * Build a yml file of fabric network, which can be loaded by {@link NetworkConfig}.
 * 
 * @author ecsoya
 *
 */
public class BbeNetworkGenerator {

	public static void main(String[] args) {
		String[] clients = { "org1", "org2" };
		File root = new File("src/main/resources/bbe/");
		try {
			for (String client : clients) {
				JsonObject json = new BbeNetworkBuilder()
						// Name of fabric network
						.name("example-fabric")
						// Current client 
						.client(client)
						// All orgs: org1, org2...
						.orgs(clients)
						// Channel
						.channel("common")
						
						// Root Directory of crypto files.
						.root(root)
						
						// IP address binding for peer of orgs, '*' means all peers of a org. 
						
						// org1 ip address
						.url("org1", "*", "106.13.184.40")
						
						// org2 ip address
						.url("org2", "*", "106.13.171.253")
						
						// orderers ip address
						.url("orderer0", null, "106.13.184.40")
						.url("orderer", null, "106.13.184.40")
						.url("orderer1", null, "106.13.171.253")
						
						// peers ip address for org2
						.url("org2", "peer0", "106.13.181.5")
						.url("org2", "peer1", "106.13.172.33")
						.url("org2", "peer2", "106.12.3.91")
						.url("org2", "peer3", "106.13.164.160")
						
						// peers ip address for org1
						.url("org1", "peer0", "106.13.161.205")
						.url("org1", "peer1", "106.12.47.80")
						.url("org1", "peer2", "106.13.172.79")
						.url("org1", "peer3", "106.12.95.182")
						
						// bind port to peers, default is 7051 for all peers and 7050 for all orderers. 
						// .port("org1", "peer1", 7051)
						
						// Build to JSON.
						.build();

				Gson gson = new Gson();
				String value = gson.toJson(json);

				// Write the network config file to yml.
				Yaml yaml = new Yaml();
				Object map = yaml.load(value);
				String yvalue = yaml.dump(map);

				// Output
				File file = new File(new File("src/main/resources/network/"), "connection-" + client + ".yml");
				if (!file.exists()) {
					Files.write(file.toPath(), yvalue.getBytes(), StandardOpenOption.CREATE_NEW,
							StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
				} else {
					Files.write(file.toPath(), yvalue.getBytes(), StandardOpenOption.TRUNCATE_EXISTING,
							StandardOpenOption.WRITE);
				}

			}
		} catch (BbeNetworkBuilderException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
