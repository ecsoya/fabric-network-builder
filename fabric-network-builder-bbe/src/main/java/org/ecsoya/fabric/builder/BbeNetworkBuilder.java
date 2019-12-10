package org.ecsoya.fabric.builder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hyperledger.fabric.sdk.NetworkConfig;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * 
 * Build a yml file of fabric network, which can be loaded by
 * {@link NetworkConfig}.
 * 
 * @author ecsoya
 *
 */
public class BbeNetworkBuilder {

	private String name;

	private String[] orgs;

	private int peers = 4;

	private String channel;

	private boolean usePem = true;

	private Map<String, Map<String, String>> urls = new HashMap<>();
	private Map<String, Map<String, Integer>> ports = new HashMap<>();
	private File root;
	private String client;

	public BbeNetworkBuilder() {
	}

	public BbeNetworkBuilder root(File root) {
		this.root = root;
		return this;
	}

	public BbeNetworkBuilder name(String name) {
		this.name = name;
		return this;
	}

	public BbeNetworkBuilder usePem(boolean usePem) {
		this.usePem = usePem;
		return this;
	}

	public BbeNetworkBuilder url(String org, String peer, String url) {
		Map<String, String> value = urls.get(org);
		if (value == null) {
			value = new HashMap<>();
			urls.put(org, value);
		}
		if (peer == null) {
			peer = org;
		}
		value.put(peer, url);
		return this;
	}

	public BbeNetworkBuilder port(String org, String peer, int port) {
		Map<String, Integer> value = ports.get(org);
		if (value == null) {
			value = new HashMap<>();
			ports.put(org, value);
		}
		if (peer == null) {
			peer = org;
		}
		value.put(peer, port);
		return this;
	}

	public BbeNetworkBuilder orgs(String... orgs) {
		this.orgs = orgs;
		return this;
	}

	public BbeNetworkBuilder channel(String channel) {
		this.channel = channel;
		return this;
	}

	public BbeNetworkBuilder client(String client) {
		this.client = client;
		return this;
	}

	public JsonObject build() throws BbeNetworkBuilderException {
		if (name == null) {
			throw new BbeNetworkBuilderException("The network name is not specified.");
		}
		if (orgs == null) {
			throw new BbeNetworkBuilderException("The client organization is not specified.");
		}

		if (channel == null || channel.equals("")) {
			throw new BbeNetworkBuilderException("The network channels is not specified.");
		}

		if (root == null || !root.exists()) {
			throw new BbeNetworkBuilderException("The network root directory is not existed.");
		}
		JsonObject root = new JsonObject();

		root.addProperty("name", name);
		root.addProperty("version", "1.0.0");
		root.addProperty("x-type", "hlfv1");

		// client
		JsonObject client = buildClient();
		root.add("client", client);

		// channels
		JsonObject channels = buildChannels();
		root.add("channels", channels);

		// organizations
		JsonObject organizations = buildOrganizations();
		root.add("organizations", organizations);

		// orderers
		JsonObject orderers = buildOrderers();
		root.add("orderers", orderers);

		// peers
		JsonObject peers = buildPeers();
		root.add("peers", peers);

		// certificateAuthorities
		JsonObject certificateAuthorities = buildCertificateAuthorities();
		root.add("certificateAuthorities", certificateAuthorities);
		return root;
	}

	private JsonObject buildCertificateAuthorities() throws BbeNetworkBuilderException {
		JsonObject root = new JsonObject();

		for (String org : orgs) {
			JsonObject node = buildCertificateAuthorityNode(org);
			root.add("ca." + org, node);
		}

		return root;
	}

	private JsonObject buildCertificateAuthorityNode(String org) throws BbeNetworkBuilderException {
		JsonObject node = new JsonObject();

		node.addProperty("url", "https://" + getUrl(org, null) + ":7054");

		JsonObject grpcOptions = new JsonObject();
		grpcOptions.addProperty("ssl-target-name-override", "ca." + org);
		grpcOptions.addProperty("allow-insecure", 0);
		grpcOptions.addProperty("trustServerCertificate", true);
		grpcOptions.addProperty("hostnameOverride", "ca." + org);
		node.add("grpcOptions", grpcOptions);

		JsonObject httpOptions = new JsonObject();
		httpOptions.addProperty("verify", false);
		node.add("httpOptions", httpOptions);

		JsonArray registrar = new JsonArray();

		JsonObject admin = new JsonObject();
		admin.addProperty("enrollId", "admin");
		admin.addProperty("enrollSecret", "adminpw");
		registrar.add(admin);

		node.add("registrar", registrar);
		JsonObject tlsCACerts = new JsonObject();
		if (!usePem) {
			tlsCACerts.addProperty("path", getCaCertPath(org));
		} else {
			tlsCACerts.addProperty("pem", getCaCertPem(org));
		}
		node.add("tlsCACerts", tlsCACerts);

		return node;
	}

	private String getUrl(String org, String peer) throws BbeNetworkBuilderException {
		Map<String, String> value = urls.get(org);
		if (value == null || value.isEmpty()) {
			throw new BbeNetworkBuilderException("Unnable to find URL for org: " + org);
		}
		if (peer == null) {
			peer = "*";
		}
		if (value.containsKey(peer)) {
			return value.get(peer);
		} else if (value.containsKey(org)) {
			return value.get(org);
		} else if (value.containsKey("*")) {
			return value.get("*");
		}
		throw new BbeNetworkBuilderException("Unnable to find URL for peer: " + peer + " in org: " + org);
	}

	private Integer getPort(String org, String peer, int defaultValue) {
		Map<String, Integer> value = ports.get(org);
		if (value == null) {
			return defaultValue;
		}
		if (peer == null) {
			peer = org;
		}
		if (value.containsKey(peer)) {
			return value.get(peer);
		}
		return defaultValue;
	}

	private List<String> getPeers() {
		return IntStream.range(0, peers).mapToObj(i -> "peer" + i).collect(Collectors.toList());
	}

	private JsonObject buildPeers() throws BbeNetworkBuilderException {
		JsonObject root = new JsonObject();

		for (String org : orgs) {
			for (String p : getPeers()) {
//				String name = p + "." + org;
				String name = org + "-" + p;
				JsonObject node = new JsonObject();
				node.addProperty("url", "grpcs://" + getUrl(org, p) + ":" + getPort("*", p, 7051));
//				node.addProperty("eventUrl", "grpcs://" + getUrl(o, "*") + ":" + getPort("*", "event_" + p, 7053));

				JsonObject grpcOptions = new JsonObject();
				grpcOptions.addProperty("ssl-target-name-override", name);
				grpcOptions.addProperty("grpc.http2.keepalive_time", 15);
				grpcOptions.addProperty("request-timeout", 120001);
//				grpcOptions.addProperty("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);
				grpcOptions.addProperty("hostnameOverride", name);
				node.add("grpcOptions", grpcOptions);

				JsonObject tlsCACerts = new JsonObject();
				if (!usePem) {
					tlsCACerts.addProperty("path", getPeerCertPath(org, p));
				} else {
					tlsCACerts.addProperty("pem", getPeerCertPem(org, p));
				}
				node.add("tlsCACerts", tlsCACerts);
				root.add(name, node);
			}
		}
		return root;
	}

	private JsonObject buildOrderers() throws BbeNetworkBuilderException {
		JsonObject node = new JsonObject();
		List<String> allOrderers = getOrderers();
//		allOrderers.add(0, ordererOrg);
		for (String org : allOrderers) {
			JsonObject orgNode = new JsonObject();
			orgNode.addProperty("url", "grpcs://" + getUrl(org, null) + ":7050");

			JsonObject grpcOptions = new JsonObject();
			grpcOptions.addProperty("grpc-max-send-message-length", 15);
			grpcOptions.addProperty("grpc.keepalive_time_ms", 360000);
			grpcOptions.addProperty("grpc.keepalive_timeout_ms", 180000);
			grpcOptions.addProperty("hostnameOverride", org);
			orgNode.add("grpcOptions", grpcOptions);

			JsonObject tlsCACerts = new JsonObject();
			if (!usePem) {
				tlsCACerts.addProperty("path", getOrdererCertPath(org));
			} else {
				tlsCACerts.addProperty("pem", getOrdererCertPem(org));
			}
			orgNode.add("tlsCACerts", tlsCACerts);
			node.add(org, orgNode);
		}
		return node;
	}

	private JsonObject buildOrganizations() throws BbeNetworkBuilderException {
		JsonObject node = new JsonObject();
//		JsonObject ordererOrgNode = buildOrgNode(ordererOrg, 0);
//		node.add(ordererOrg, ordererOrgNode);

		for (int i = 0; i < orgs.length; i++) {
			String org = orgs[i];
			JsonObject child = buildOrgNode(org, peers);
			node.add(org, child);
		}
		return node;
	}

	private JsonObject buildOrgNode(String org, int numOfPeers) throws BbeNetworkBuilderException {
		JsonObject ordererOrgNode = new JsonObject();
		ordererOrgNode.addProperty("mspid", org + "MSP");

		JsonArray certificateAuthorities = new JsonArray();
		certificateAuthorities.add("ca." + org);
		ordererOrgNode.add("certificateAuthorities", certificateAuthorities);

		JsonObject adminPrivateKey = new JsonObject();
		if (!usePem) {
			adminPrivateKey.addProperty("path", getAdminPrivateKeyPath(org));
		} else {
			adminPrivateKey.addProperty("pem", getAdminPrivateKeyPem(org));
		}
		ordererOrgNode.add("adminPrivateKey", adminPrivateKey);

		JsonObject signedCert = new JsonObject();
		if (!usePem) {
			signedCert.addProperty("path", getAdminCertPath(org));
		} else {
			signedCert.addProperty("pem", getAdminCertPem(org));
		}
		ordererOrgNode.add("signedCert", signedCert);
		if (numOfPeers > 0) {
			JsonArray peers = new JsonArray();
			for (int i = 0; i < numOfPeers; i++) {
//				peers.add("peer" + i + "." + org);
				peers.add(org + "-" + "peer" + i);
			}
			ordererOrgNode.add("peers", peers);
		}
		return ordererOrgNode;
	}

	private String getCaCertPath(String org) throws BbeNetworkBuilderException {
		return org + "/ca/ca." + org + "-cert.pem";
	}

	private String getCaCertPem(String org) throws BbeNetworkBuilderException {
		File file = new File(root, getCaCertPath(org));
		try {
			return new String(Files.readAllBytes(file.toPath()));
		} catch (IOException e) {
//			throw new NetworkBuilderException(e);
			return null;
		}
	}

	private String getOrdererCertPem(String org) throws BbeNetworkBuilderException {
		File file = new File(root, getOrdererCertPath(org));
		try {
			return new String(Files.readAllBytes(file.toPath()));
		} catch (IOException e) {
			throw new BbeNetworkBuilderException(e);
		}
	}

	private String getOrdererCertPath(String org) {
		return "org-orderer/orderers/" + org + "/msp/tlscacerts/tlsca.org-orderer-cert.pem";
	}

	private String getPeerCertPem(String org, String peer) throws BbeNetworkBuilderException {
		File file = new File(root, getPeerCertPath(org, peer));
		try {
			return new String(Files.readAllBytes(file.toPath()));
		} catch (IOException e) {
			throw new BbeNetworkBuilderException(e);
		}
	}

	private String getPeerCertPath(String org, String peer) {
		return org + "/peers/" + peer + "/msp/tlscacerts/tlsca." + org + "-cert.pem";
	}

	private String getAdminCertPem(String org) throws BbeNetworkBuilderException {
		File file = new File(root, getAdminCertPath(org));

		try {
			return new String(Files.readAllBytes(file.toPath()));
		} catch (IOException e) {
//			throw new NetworkBuilderException(e);
			return null;
		}
	}

	private String getAdminCertPath(String org) {
		return org + "/users/Admin@" + org + "/msp/admincerts/Admin@" + org + "-cert.pem";
	}

	private String getAdminPrivateKeyPem(String org) throws BbeNetworkBuilderException {
		File dir = new File(root, getAdminPrivateKeyPath(org));
		if (!dir.exists()) {
			return null;
//			throw new NetworkBuilderException("Can not find private key for " + org);
		}
		File[] listFiles = dir.listFiles();
		if (listFiles.length == 0) {
			return null;
		}
		File keyFile = listFiles[0];
		try {
			return new String(Files.readAllBytes(keyFile.toPath()));
		} catch (IOException e) {
			throw new BbeNetworkBuilderException(e);
		}
	}

	private String getAdminPrivateKeyPath(String org) {
		return org + "/users/Admin@" + org + "/msp/keystore";
	}

	private List<String> getOrderers() {
		return IntStream.range(0, orgs.length).mapToObj(i -> "orderer" + i).collect(Collectors.toList());
	}

	private JsonObject buildChannels() {
		JsonObject channelsNode = new JsonObject();

		JsonObject node = new JsonObject();
		// orderers
		JsonArray orderers = new JsonArray();
//			orderers.add(ordererOrg );
		for (String orderer : getOrderers()) {
			orderers.add(orderer);
		}
		node.add("orderers", orderers);

		// peers
		JsonObject peersNode = new JsonObject();
		for (String org : orgs) {
			for (String peer : getPeers()) {

				JsonObject o = new JsonObject();
				if (peer.equals("peer0")) {
					o.addProperty("endorsingPeer", true);
					o.addProperty("chaincodeQuery", true);
					o.addProperty("ledgerQuery", true);
					o.addProperty("eventSource", true);
				} else if (peer.equals("peer1")) {
					o.addProperty("endorsingPeer", false);
					o.addProperty("chaincodeQuery", true);
					o.addProperty("ledgerQuery", false);
					o.addProperty("eventSource", false);
				} else if (peer.equals("peer2")) {
					o.addProperty("endorsingPeer", false);
					o.addProperty("chaincodeQuery", false);
					o.addProperty("ledgerQuery", true);
					o.addProperty("eventSource", false);
				} else if (peer.equals("peer3")) {
					o.addProperty("endorsingPeer", false);
					o.addProperty("chaincodeQuery", false);
					o.addProperty("ledgerQuery", false);
					o.addProperty("eventSource", true);
				}
//				peersNode.add(peer + "." + org, o);
				peersNode.add(org + "-" + peer, o);
			}

		}
		node.add("peers", peersNode);

		// policies
		JsonObject policies = new JsonObject();
		JsonObject queryChannelConfig = new JsonObject();
		queryChannelConfig.addProperty("minResponses", 1);
		queryChannelConfig.addProperty("maxTargets", 1);

		JsonObject retryOpts = new JsonObject();
		retryOpts.addProperty("attempts", 5);
		retryOpts.addProperty("initialBackoff", "500ms");
		retryOpts.addProperty("maxBackoff", "5s");
		retryOpts.addProperty("backoffFactor", "2.0");
		queryChannelConfig.add("retryOpts", retryOpts);
		node.add("policies", policies);
		channelsNode.add(channel, node);

		return channelsNode;
	}

	private JsonObject buildClient() {
		JsonObject client = new JsonObject();

		JsonObject logging = new JsonObject();
		logging.addProperty("level", "debug");
		client.add("logging", logging);

		JsonObject connection = new JsonObject();
		JsonObject timeout = new JsonObject();
		JsonObject peer = new JsonObject();
		peer.addProperty("endorser", 30000);
		peer.addProperty("eventHub", 30000);
		peer.addProperty("eventReg", 30000);
		timeout.add("peer", peer);
		timeout.addProperty("orderer", 30000);

		connection.add("timeout", timeout);
		client.add("connection", connection);

		client.addProperty("organization", this.client);

//		JsonObject credentialStore = new JsonObject();
//		credentialStore.addProperty("path", "tmp/hfc-kvs");
//
//		JsonObject cryptoStore = new JsonObject();
//		cryptoStore.addProperty("path", "tmp/hfc-cvs");
//		credentialStore.add("cryptoStore", cryptoStore);
//
//		credentialStore.addProperty("wallet", "cellshop");
//		client.add("credentialStore", credentialStore);
		return client;
	}

}
