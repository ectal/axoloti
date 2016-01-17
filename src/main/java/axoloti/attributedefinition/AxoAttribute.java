/**
 * Copyright (C) 2013 - 2016 Johannes Taelman
 *
 * This file is part of Axoloti.
 *
 * Axoloti is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Axoloti is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Axoloti. If not, see <http://www.gnu.org/licenses/>.
 */
package axoloti.attributedefinition;

/**
 *
 * @author Johannes Taelman
 */
import axoloti.atom.AtomDefinition;
import axoloti.attribute.AttributeInstance;
import axoloti.object.AxoObjectInstance;
import axoloti.utils.CharEscape;
import java.security.MessageDigest;
import org.simpleframework.xml.Attribute;

public abstract class AxoAttribute implements AtomDefinition {

    @Attribute
    String name;

    public AxoAttribute() {
    }

    public AxoAttribute(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public AttributeInstance CreateInstance(AxoObjectInstance o) {
        // resolve deserialized object, copy value and remove
        AttributeInstance pidn = null;
        for (AttributeInstance pi : o.getAttributeInstances()) {
//            System.out.println("compare " + this.name + "<>" + pi.name);
            if (pi.getAttributeName().equals(this.name)) {
                /*
                 if (InstanceFactory().getClass().isInstance(pi)) {
                 pidn = (AttributeInstance) pi;
                 } else {
                 o.getAttributeInstances().remove(pi);
                 }*/
                pidn = (AttributeInstance) pi;
                break;
            }
        }
        if (pidn == null) {
//            System.out.println("no match " + this.name);
            AttributeInstance pi = InstanceFactory(o);
            o.add(pi);
            pi.PostConstructor();
            return pi;
        } else {
//            System.out.println("match" + pidn.getName());
            o.getAttributeInstances().remove(pidn);
            AttributeInstance pi = InstanceFactory(o);
            pi.CopyValueFrom(pidn);
            o.add(pi);
            pi.PostConstructor();
            return pi;
        }
    }

    public abstract AttributeInstance InstanceFactory(AxoObjectInstance o);

    public void updateSHA(MessageDigest md) {
        md.update(name.getBytes());
    }

    public String GetCName() {
        return "attr_" + CharEscape.CharEscape(name);
    }

}
