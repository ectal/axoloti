/**
 * Copyright (C) 2013, 2014 Johannes Taelman
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
package axoloti.parameters;

import axoloti.MainFrame;
import static axoloti.PatchViewType.PICCOLO;
import axoloti.datatypes.Value;
import axoloti.objectviews.AxoObjectInstanceView;
import axoloti.objectviews.IAxoObjectInstanceView;
import axoloti.parameterviews.IParameterInstanceView;
import axoloti.parameterviews.ParameterInstanceView4LevelX16;
import axoloti.piccolo.objectviews.PAxoObjectInstanceView;
import axoloti.piccolo.parameterviews.PParameterInstanceView4LevelX16;
import org.simpleframework.xml.Attribute;

/**
 *
 * @author Johannes Taelman
 */
public class ParameterInstance4LevelX16 extends ParameterInstanceInt32 {

    public ParameterInstance4LevelX16() {
    }

    public ParameterInstance4LevelX16(@Attribute(name = "value") int v) {
        super(v);
    }

    @Override
    public String GenerateCodeMidiHandler(String vprefix) {
        return "";
    }

    @Override
    public void setValue(Value value) {
        super.setValue(value);
    }

    @Override
    public IParameterInstanceView getViewInstance(IAxoObjectInstanceView o) {
        if (MainFrame.prefs.getPatchViewType() == PICCOLO) {
            return new PParameterInstanceView4LevelX16(this, (PAxoObjectInstanceView) o);
        } else {
            return new ParameterInstanceView4LevelX16(this, (AxoObjectInstanceView) o);
        }
    }
}
