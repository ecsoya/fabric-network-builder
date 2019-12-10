package org.ecsoya.fabric.builder;

import java.io.File;
import java.io.IOException;

import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;

public class BbeNetworkTest {

	public static void main(String[] args) {
		File file = new File("src/main/resources/bbe/network/connection.yml");

		try {
			NetworkConfig network = NetworkConfig.fromYamlFile(file);
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
