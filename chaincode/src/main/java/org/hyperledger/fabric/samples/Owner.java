package org.hyperledger.fabric.samples;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.Objects;

@DataType()
public class Owner {
    @Property()
    private String org;

    @Property()
    private String user;


    public String getOrg() {
        return org;
    }

    public String getUser() {
        return user;
    }

    public Owner(@JsonProperty("org") final String org) {
        this.org = org;
        this.user= " ";

    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        Owner other = (Owner) obj;

        return Objects.deepEquals(
                new String[] {getOrg(), getUser()},
                new String[] {other.getOrg(), other.getUser()});
    };

    @Override
    public int hashCode() {
        return Objects.hash(getOrg(), getUser());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [ Org=" + org + ", User=" + user  + "]";
    }
}
