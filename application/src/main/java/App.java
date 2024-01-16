/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.google.gson.stream.JsonReader;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;

public final class App {

	private static final Path PATH_TO_TEST_NETWORK = Paths.get("..", "..", "fabric-samples", "test-network");

	private static final String CHANNEL_NAME = System.getenv().getOrDefault("CHANNEL_NAME", "mychannel");
	private static final String CHAINCODE_NAME = System.getenv().getOrDefault("CHAINCODE_NAME", "basic");

	// Gateway peer end point.
	private static final String PEER_ENDPOINT = "localhost:7051";
	private static final String OVERRIDE_AUTH = "peer0.org1.example.com";

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();



	public static void main(final String[] args) throws Exception {
		
		ChannelCredentials credentials = TlsChannelCredentials.newBuilder()
				.trustManager(PATH_TO_TEST_NETWORK.resolve(Paths.get(
						"organizations/peerOrganizations/org1.example.com/" +
								"peers/peer0.org1.example.com/tls/ca.crt"))
						.toFile())
				.build();
		// The gRPC client connection should be shared by all Gateway connections to
		// this endpoint.
		ManagedChannel channel = Grpc.newChannelBuilder(PEER_ENDPOINT, credentials)
				.overrideAuthority(OVERRIDE_AUTH)
				.build();
		
		Gateway.Builder builderOrg1 = Gateway.newInstance()
				.identity(new X509Identity("Org1MSP",
						Identities.readX509Certificate(
							Files.newBufferedReader(
								PATH_TO_TEST_NETWORK.resolve(Paths.get(
									"organizations/peerOrganizations/org1.example.com/" +
									"users/User1@org1.example.com/msp/signcerts/cert.pem"
								))
							)
						)
					))
				.signer(
					Signers.newPrivateKeySigner(
						Identities.readPrivateKey(
							Files.newBufferedReader(
								Files.list(PATH_TO_TEST_NETWORK.resolve(
									Paths.get(
										"organizations/peerOrganizations/org1.example.com/" +
										"users/User1@org1.example.com/msp/keystore")
									)
								).findFirst().orElseThrow()
							)
						)
					)
				)
				.connection(channel)
				// Default timeouts for different gRPC calls
				.evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
				.endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
				.submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
				.commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));

		// notice that we can share the grpc connection since we don't use private date,
		// otherwise we should create another connection
		Gateway.Builder builderOrg2 = Gateway.newInstance()
				.identity(new X509Identity("Org2MSP",
						Identities.readX509Certificate(Files.newBufferedReader(PATH_TO_TEST_NETWORK.resolve(Paths.get(
								"organizations/peerOrganizations/org2.example.com/users/User1@org2.example.com/msp/signcerts/cert.pem"))))))
				.signer(Signers.newPrivateKeySigner(Identities.readPrivateKey(Files.newBufferedReader(Files
						.list(PATH_TO_TEST_NETWORK.resolve(Paths
								.get("organizations/peerOrganizations/org2.example.com/users/User1@org2.example.com/msp/keystore")))
						.findFirst().orElseThrow()))))
				.connection(channel)
				.evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
				.endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
				.submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
				.commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));
		Scanner scanner = new Scanner(System.in);

		try (Gateway gatewayOrg1 = builderOrg1.connect();
				Gateway gatewayOrg2 = builderOrg2.connect()) {
			
			Contract contractOrg1 = gatewayOrg1
				.getNetwork(CHANNEL_NAME)
				.getContract(CHAINCODE_NAME);
			
			Contract contractOrg2 = gatewayOrg2
				.getNetwork(CHANNEL_NAME)
				.getContract(CHAINCODE_NAME);


			//ORGS
			Map<String, Contract> ORGS = new HashMap<>();
			ORGS.put("Pittaluga & fratelli", contractOrg1);
			ORGS.put("Supermarket", contractOrg2);


			//Transactions
			Map<String, String> PF_TRANSACTIONS = new HashMap<>();
			PF_TRANSACTIONS.put("CreateTracking","Create a new plant tracking");
			PF_TRANSACTIONS.put("StopTracking","Delete a plant tracking");
			PF_TRANSACTIONS.put("UpdateTracking","Update the actual state of a plant");
			PF_TRANSACTIONS.put("GetActualTracking","Get the actual state of the plant");
			PF_TRANSACTIONS.put("GetHistory","Get the history of a plant");
			PF_TRANSACTIONS.put("TransferTracking","Transfer ownership of a plant");

			Map<String, String> S_TRANSACTIONS = new HashMap<>();
			S_TRANSACTIONS.put("StopTracking","Delete a plant tracking");
			S_TRANSACTIONS.put("GetHistory","Get the history of a plant");


			while (true) {
				try {
					String orgName=getOrgIndex(ORGS.keySet().toArray(new String[0]), scanner);
					if (orgName == null) continue;
					Contract orgContract = ORGS.get(orgName);


					String txKey;
					String txName;
					if(orgName=="Pittaluga & fratelli"){
						txKey = getTransactionName(PF_TRANSACTIONS,scanner);
						txName=PF_TRANSACTIONS.get(txKey);

					}else{
						txKey = getTransactionName(S_TRANSACTIONS,scanner);
						txName= S_TRANSACTIONS.get(txKey);
					}
					byte[] result;

					System.out.println(String.format("Please follow below steps to complete transaction: %s",txName));
					switch (orgName){
						case "Pittaluga & fratelli":
							switch (txKey){
								case "CreateTracking":
									System.out.print("Insert qr code: ");
									String newQr = scanner.next();
									System.out.print("Insert extraInfo: ");
									String extraInfo = scanner.next();
									System.out.print("Insert gpsPosition: ");
									String gpsPosition = scanner.next();
									result = orgContract.submitTransaction(txKey, newQr, extraInfo, gpsPosition);
									System.out.println("Result -> " + new String(result));
									break;
								case "StopTracking":
									System.out.print("Insert qr code: ");
									String stopTrackingQr = scanner.next();
									result = orgContract.evaluateTransaction(txKey, stopTrackingQr);
									System.out.println("Result -> " + new String((result)));
									break;
								case"UpdateTracking":
									System.out.print("Insert qr code: ");
									String existingQr = scanner.next();
									System.out.print("Insert new gpsPosition: ");
									String gpsPositionUpdate = scanner.next();
									result = orgContract.submitTransaction(txKey, existingQr, gpsPositionUpdate);
									System.out.println("Result -> " + new String(result));
									break;
								case "GetActualTracking":
									System.out.print("Insert qr code: ");
									String qrActual = scanner.next();
									result = orgContract.evaluateTransaction(txKey, qrActual);
									System.out.println("Result -> " + new String(result));
									break;
								case "GetHistory":
									System.out.print("Insert qr code: ");
									String qr = scanner.next();
									result = orgContract.evaluateTransaction(txKey, qr);
									System.out.println("Result -> " + prettyJson(result));
									break;
								case "TransferTracking":
									System.out.print("Insert qr code: ");
									String transferCarId = scanner.next();
									System.out.println("Insert the new owner: ");
									String newOwner = getOrgIndex(ORGS.keySet().toArray(new String[0]), scanner);
									if(newOwner==orgName){
										System.out.println(String.format("You choosed again the same owner %s !",orgName));
									}else{
										result = orgContract.submitTransaction(txKey, transferCarId, newOwner=="Pittaluga & fratelli" ? "ORG1MSP":"ORG2MSP");
										System.out.println("Result -> " + new String(result));
									}
									break;
							}

							break;
						case "Supermarket":
							switch (txKey){
								case "StopTracking":
									System.out.print("Insert qr code: ");
									String stopTrackingQr = scanner.next();
									result = orgContract.evaluateTransaction(txKey, stopTrackingQr);
									System.out.println("Result -> " + new String((result)));
									break;
								case "GetHistory":
									System.out.print("Insert qr code: ");
									String qr = scanner.next();
									result = orgContract.evaluateTransaction(txKey, qr);
									System.out.println("result = " + prettyJson(result));
									break;
							}
							break;
					}

				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
			}
		} finally {
			channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
			scanner.close();
		}
	}

	private static String getOrgIndex(String[] ORGS, Scanner scanner) {
		System.out.println("Choose an organization: ");
		for (int i = 0; i < ORGS.length; i++) {
			System.out.print(i+1 + ": " + ORGS[i] + "\n");
		}
		System.out.print(">>");
		int orgIndex = scanner.nextInt();
		if (orgIndex < 1 || orgIndex >= ORGS.length+1) {
			System.err.println("Wrong org index");
			return null;
		}
		return ORGS[orgIndex-1];
	}

	private static String getTransactionName(Map<String,String> transactions, Scanner scanner) {
		System.out.println("Choose a transaction :");

		String[] keySet=transactions.keySet().toArray(new String[0]);
		for (int i = 0; i < keySet.length; i++) {
			System.out.print(i+1 + ": " + transactions.get(keySet[i]) + "\n");
		}

		System.out.print(">>");
		int txIndex = scanner.nextInt();
		if (txIndex < 1 || txIndex >= keySet.length+1) {
			System.err.println("Wrong transaction index");
			return null;
		}
		return keySet[txIndex-1];
	}


	private static String prettyJson(final byte[] json) {
		return prettyJson(new String(json, StandardCharsets.UTF_8));
	}

	private static String prettyJson(final String json) {
		var parsedJson = JsonParser.parseString(json);
		return gson.toJson(parsedJson);
	}
}
