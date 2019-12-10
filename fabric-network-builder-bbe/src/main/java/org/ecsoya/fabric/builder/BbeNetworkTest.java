package org.ecsoya.fabric.builder;

import java.io.File;
import java.io.IOException;

import org.ecsoya.fabric.network.FabricNetwork;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;

public class BbeNetworkTest {

	public static void main(String[] args) {
		File file = new File("src/main/resources/bbe/network/connection.yml");

		try {
			FabricNetwork network = FabricNetwork.fromYamlFile(file);
			System.out.println(network);
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		} catch (NetworkConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
