package org.hyperledger.fabric.samples;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.Objects;

@DataType()
public final class Basil {

    @Property()
    private String qr;

    @Property()
    private String extraInfo;

    @Property()
    private String owner;

    @Property()
    private String basilLeg;

    public String getQr() {
        return qr;
    }


    public String getExtraInfo() {
        return extraInfo;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }


    public String getBasilLeg() {
        return basilLeg;
    }

    public void setBasilLeg(String basilLeg) {
        this.basilLeg = basilLeg;
    }


    public Basil(@JsonProperty("qr") final String qr, @JsonProperty("extraInfo") final String exraInfo,
                 @JsonProperty("owner") final String owner, @JsonProperty("basilLeg") final String basilLeg) {
        this.qr = qr;
        this.extraInfo = exraInfo;
        this.owner = owner;
        this.basilLeg=basilLeg;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        Basil other = (Basil) obj;

        return Objects.deepEquals(
                new String[] {getQr(),getExtraInfo(), getOwner()},
                new String[] {other.getQr(), other.getExtraInfo(), other.getOwner().toString()});

    };

    @Override
    public int hashCode() {
        return Objects.hash(getQr(), getExtraInfo(), getOwner());
    }

    @Override
    public String toString() {
        return " { Qr=" + qr + ", Extra Info=" + extraInfo + ", Owner=" + owner  + ", BasilLeg=" + basilLeg + " }";
    }
}