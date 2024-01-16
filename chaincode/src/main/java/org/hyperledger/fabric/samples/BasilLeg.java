package org.hyperledger.fabric.samples;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.Objects;
import java.util.UUID;

@DataType()
public class BasilLeg {

    @Property()
    private String id;

    @Property()
    private long timestamp;

    @Property()
    private String gpsPosition;

    @Property()
    private String basil;

    public String getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getGpsPosition() {
        return gpsPosition;
    }

    public String getBasil() {
        return basil;
    }


    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setGpsPosition(String gpsPosition) {
        this.gpsPosition = gpsPosition;
    }

    public BasilLeg(@JsonProperty("timestamp") final long timestamp, @JsonProperty("gpsPosition") final String gpsPosition,
                    @JsonProperty("basil") final String basil) {
        this.id = "BasilLeg: " + timestamp;
        this.timestamp = timestamp;
        this.gpsPosition = gpsPosition;
        this.basil = basil;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        BasilLeg other = (BasilLeg) obj;

        return Objects.deepEquals(
                new String[] {getId(), String.valueOf(getTimestamp()), getGpsPosition()},
                new String[] {other.getId(), String.valueOf(other.getTimestamp()), other.getBasil()});

    };


    @Override
    public int hashCode() {
        return Objects.hash(getId(), getTimestamp(), getGpsPosition(), getBasil());
    }

    @Override
    public String toString() {
        return  " { ID=" + id + ", Timestamp=" + timestamp + ", Gps Position=" + gpsPosition  +  ", Basil=" + basil  + " }";
    }
}
