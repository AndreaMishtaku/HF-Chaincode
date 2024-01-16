package org.hyperledger.fabric.samples;


import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.*;

@Contract(
        name = "basic",
        info = @Info(
                title = "Asset Transfer",
                description = "The hyperlegendary asset transfer",
                version = "0.0.1-SNAPSHOT",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "a.transfer@example.com",
                        name = "Adrian Transfer",
                        url = "https://hyperledger.example.com")))
@Default
public final class BasilContract implements ContractInterface {

    private final Genson genson = new Genson();

    private enum AssetTransferErrors {
        BASIL_NOT_FOUND,
        BASIL_ALREADY_EXISTS,
        NOT_THE_OWNER,
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String CreateTracking(final Context ctx, final String qr, final String extraInfo,final String gpsPosition) {
        ChaincodeStub stub = ctx.getStub();

        if (checkIfExists(ctx, qr)) {
            String errorMessage = String.format("Basil %s already exists", qr);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.BASIL_ALREADY_EXISTS.toString());
        }

        String submittingOrg = ctx.getClientIdentity().getMSPID();

        BasilLeg basilLeg= new BasilLeg(ctx.getStub().getTxTimestamp().getEpochSecond(), gpsPosition, qr);

        System.out.println(basilLeg);

        String basilLegJson=genson.serialize(basilLeg);
        stub.putStringState(basilLeg.getId(), basilLegJson);

        Basil newBasil = new Basil(qr,extraInfo,submittingOrg,basilLeg.getId());

        System.out.println();
        System.out.println("CreateTracking: basil.getOwner() = " + newBasil.getOwner());
        System.out.println();

        String basilJson = genson.serialize(newBasil);
        stub.putStringState(qr, basilJson);

        return "Plant created successfully";
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String StopTracking(final Context ctx, final String qr) {
        ChaincodeStub stub = ctx.getStub();

        if (!checkIfExists(ctx, qr)) {
            String errorMessage = String.format("Basil %s does not exist", qr);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.BASIL_NOT_FOUND.toString());
        }

        String submittingOrg = ctx.getClientIdentity().getMSPID();

        String basilJSON = stub.getStringState(qr);
        Basil basil = genson.deserialize(basilJSON, Basil.class);


        if (!basil.getOwner().equals(submittingOrg)) {
            String errorMessage = "Action not allowed because doesnt correspond to the owner";
            throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_THE_OWNER.toString());
        }

        stub.delState(qr);
        stub.delState(basil.getBasilLeg());
        return "Plant deleted successfully";
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String UpdateTracking(final Context ctx, final String qr, final String gpsPosition) {
        ChaincodeStub stub = ctx.getStub();

        if (!checkIfExists(ctx, qr)) {
            String errorMessage = String.format("Basil %s does not exist", qr);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.BASIL_NOT_FOUND.toString());
        }

        String submittingOrg = ctx.getClientIdentity().getMSPID();

        String basilJSON = stub.getStringState(qr);
        Basil basil = genson.deserialize(basilJSON, Basil.class);

        stub.delState(basil.getBasilLeg());

        if (!basil.getOwner().equals(submittingOrg)) {
            String errorMessage = "Action not allowed because doesnt correspond to the owner";
            throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_THE_OWNER.toString());
        }


        BasilLeg basilLeg = new BasilLeg(ctx.getStub().getTxTimestamp().getEpochSecond(),gpsPosition,basil.getQr());

        basil.setBasilLeg(basilLeg.getId());
        stub.putStringState(basil.getQr(),genson.serialize(basil));

        stub.putStringState(basilLeg.getId(), genson.serialize(basilLeg));

        return "Plant updated successfully";
    }


    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetActualTracking(final Context ctx, final String qr) {
        ChaincodeStub stub = ctx.getStub();
        String basilJSON = stub.getStringState(qr);

        if (basilJSON== null || basilJSON.isEmpty()) {
            String errorMessage = String.format("Basil %s does not exist", qr);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.BASIL_NOT_FOUND.toString());
        }

        Basil basil = genson.deserialize(basilJSON, Basil.class);

        System.out.println(basil);
        String basilLegJSON = stub.getStringState(basil.getBasilLeg());
        BasilLeg basilLeg = genson.deserialize(basilLegJSON, BasilLeg.class);

        Map<String, Object> response = new HashMap<>();
        response.put("Basil", basil);
        response.put("BasilLeg", basilLeg);

        return response.toString();
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetHistory(final Context ctx, final String qr) {
        ChaincodeStub stub = ctx.getStub();

        String basilJSON = stub.getStringState(qr);
        if (basilJSON == null || basilJSON.isEmpty()) {
            String errorMessage = String.format("Basil %s does not exist", qr);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.BASIL_NOT_FOUND.toString());
        }

        List<String> result=new ArrayList<String>();

        QueryResultsIterator<KeyModification> historyForKey = stub.getHistoryForKey(qr);


        for (KeyModification keyModification : historyForKey) {
            String transaction = keyModification.getStringValue();

            result.add(transaction);
        }
        return result.toString();

    }


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String TransferTracking(final Context ctx, final String qr, String newOwner) {
        ChaincodeStub stub = ctx.getStub();
        if (!checkIfExists(ctx, qr)) {
            String errorMessage = String.format("Basil %s does not exist", qr);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.BASIL_NOT_FOUND.toString());
        }

        String submittingOrg = ctx.getClientIdentity().getMSPID();

        String basilJSON = stub.getStringState(qr);
        Basil basil = genson.deserialize(basilJSON, Basil.class);


        if (!basil.getOwner().equals(submittingOrg)) {
            String errorMessage = "Action not allowed because doesnt correspond to the owner";
            throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_THE_OWNER.toString());
        }

        basil.setOwner(newOwner);

        stub.putStringState(qr, genson.serialize(basil));

        return "Plant changed the owner";
    }


    private boolean checkIfExists(final Context ctx, final String id) {
        ChaincodeStub stub = ctx.getStub();
        String assetJSON = stub.getStringState(id);

        return (assetJSON != null && !assetJSON.isEmpty());
    }

}
