package axoloti.target;

import axoloti.chunks.ChunkData;
import axoloti.chunks.FourCCs;
import axoloti.connection.IConnection;
import axoloti.mvc.AbstractModel;
import axoloti.mvc.IModel;
import axoloti.property.BooleanProperty;
import axoloti.property.IntegerProperty;
import axoloti.property.ListProperty;
import axoloti.property.ObjectProperty;
import axoloti.property.Property;
import axoloti.property.StringProperty;
import axoloti.swingui.MainFrame;
import axoloti.target.fs.SDCardInfo;
import axoloti.target.midimonitor.MidiMonitorData;
import axoloti.target.midirouting.MidiInputRoutingTable;
import axoloti.target.midirouting.MidiOutputRoutingTable;
import axoloti.utils.FirmwareID;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import qcmds.QCmdMemRead;
import qcmds.QCmdProcessor;
import qcmds.QCmdStartFlasher;
import qcmds.QCmdStop;
import qcmds.QCmdUploadFWSDRam;
import qcmds.QCmdUploadPatch;

/**
 *
 * @author jtaelman
 */
public class TargetModel extends AbstractModel {

    private static TargetModel targetModel;

    public static TargetModel getTargetModel() {
        if (targetModel == null) {
            targetModel = new TargetModel();
        }
        return targetModel;
    }

    public TargetModel() {
        updateLinkFirmwareID();
    }

    private IConnection connection;

//    QCmdProcessor qCmdProcessor = QCmdProcessor.getQCmdProcessor();

    private String linkFirmwareID;
    private List<MidiInputRoutingTable> inputRoutingTables = Collections.emptyList();
    private List<MidiOutputRoutingTable> outputRoutingTables = Collections.emptyList();
    private boolean sDCardMounted;
    private TargetRTInfo RTInfo;
    private SDCardInfo sdcardInfo = new SDCardInfo();
    private MidiMonitorData midiMonitor;
    private String patchName;
    private int patchIndex;
    public boolean warnedAboutFWCRCMismatch = false;

    void readInputMapFromTarget() {
        ChunkData chunk_input = connection.getFWChunks().getOne(FourCCs.FW_MIDI_INPUT_ROUTING);
        ByteBuffer data = chunk_input.getData();
        int n_input_interfaces = data.remaining() / 4;
        List<MidiInputRoutingTable> cirs = new ArrayList<>(n_input_interfaces);
        for (int i = 0; i < n_input_interfaces; i++) {
            int addr = data.getInt();
            cirs.add(new MidiInputRoutingTable(addr));
        }
        CompletionHandler ch = new CompletionHandler() {
            int i = 0;

            @Override
            public void done() {
                System.out.println("ch " + i);
                if (i < n_input_interfaces) {
                    cirs.get(i).retrieve(connection, this);
                    i++;
                } else {
                    System.out.println("ch done " + i);
                    setInputRoutingTable(cirs);
                }
            }

        };
        ch.done();
    }

    void readOutputMapFromTarget() {
        ChunkData chunk_output = connection.getFWChunks().getOne(FourCCs.FW_MIDI_OUTPUT_ROUTING);
        ByteBuffer data = chunk_output.getData();
        int n_output_interfaces = data.remaining() / 4;
        List<MidiOutputRoutingTable> cors = new ArrayList<>(n_output_interfaces);
        for (int i = 0; i < n_output_interfaces; i++) {
            int addr = data.getInt();
            cors.add(new MidiOutputRoutingTable(addr));
        }
        CompletionHandler ch = new CompletionHandler() {
            int i = 0;

            @Override
            public void done() {
                System.out.println("ch " + i);
                if (i < n_output_interfaces) {
                    cors.get(i).retrieve(connection, this);
                    i++;
                } else {
                    System.out.println("ch done " + i);
                    setOutputRoutingTable(cors);
                }
            }

        };
        ch.done();

    }

    public void readFromTarget() {
        readInputMapFromTarget();
        readOutputMapFromTarget();
    }

    public void applyToTarget() {
        for (MidiInputRoutingTable mirt : inputRoutingTables) {
            mirt.apply(connection);
        }
        for (MidiOutputRoutingTable mort : outputRoutingTables) {
            mort.apply(connection);
        }
    }

    public final static Property CONNECTION = new ObjectProperty("Connection", IConnection.class, TargetModel.class);
    public final static Property FIRMWARE_LINK_ID = new StringProperty("FirmwareLinkID", TargetModel.class);
    public final static Property MRTS_INPUT = new ListProperty("InputRoutingTable", TargetModel.class);
    public final static Property MRTS_OUTPUT = new ListProperty("OutputRoutingTable", TargetModel.class);
    public final static Property HAS_SDCARD = new BooleanProperty("SDCardMounted", TargetModel.class);
    public final static Property RTINFO = new ObjectProperty("RTInfo", TargetRTInfo.class, TargetModel.class);
    public final static Property PATCHINDEX = new IntegerProperty("PatchIndex", TargetModel.class);
    public final static Property WARNEDABOUTFWCRCMISMATCH = new BooleanProperty("WarnedAboutFWCRCMismatch", TargetModel.class);
    public final static Property SDCARDINFO = new ObjectProperty("SDCardInfo", SDCardInfo.class, TargetModel.class);
    public final static Property MIDIMONITOR = new ObjectProperty("MidiMonitor", MidiMonitorData.class, TargetModel.class);
    public final static Property PATCHNAME = new StringProperty("PatchName", TargetModel.class);

    @Override
    public List<Property> getProperties() {
        List<Property> l = new ArrayList<>();
        l.add(CONNECTION);
        l.add(FIRMWARE_LINK_ID);
        l.add(MRTS_INPUT);
        l.add(MRTS_OUTPUT);
        l.add(RTINFO);
        l.add(PATCHINDEX);
        l.add(PATCHNAME);
        return l;
    }

    public List<MidiInputRoutingTable> getInputRoutingTable() {
        return Collections.unmodifiableList(inputRoutingTables);
    }

    public void setInputRoutingTable(List<MidiInputRoutingTable> routingTable) {
        this.inputRoutingTables = routingTable;
        firePropertyChange(MRTS_INPUT,
                null, routingTable);
    }

    public List<MidiOutputRoutingTable> getOutputRoutingTable() {
        return Collections.unmodifiableList(outputRoutingTables);
    }

    public void setOutputRoutingTable(List<MidiOutputRoutingTable> routingTable) {
        this.outputRoutingTables = routingTable;
        firePropertyChange(MRTS_OUTPUT,
                null, routingTable);
    }

    public IConnection getConnection() {
        return connection;
    }

    public void setConnection(IConnection connection) {
        if ((connection != null) && (this.connection == null)) {
            setSDCardInfo(new SDCardInfo());
        }
        this.connection = connection;
        firePropertyChange(CONNECTION,
                null, connection);
    }

    public String getFirmwareLinkID() {
        return linkFirmwareID;
    }

    public void setFirmwareLinkID(String linkFirmwareID) {
        this.linkFirmwareID = linkFirmwareID;
        firePropertyChange(FIRMWARE_LINK_ID,
                null, linkFirmwareID);
    }

    public final void updateLinkFirmwareID() {
        setFirmwareLinkID(FirmwareID.getFirmwareID());
    }

    public void flashUsingSDRam(String fname_flasher, String fname_fw) {
        updateLinkFirmwareID();
        File p = new File(fname_fw);
        if (p.canRead()) {
            QCmdProcessor.getQCmdProcessor().appendToQueue(new QCmdStop());
            QCmdProcessor.getQCmdProcessor().appendToQueue(new QCmdUploadFWSDRam(p));
            QCmdProcessor.getQCmdProcessor().appendToQueue(new QCmdUploadPatch(fname_flasher));
            QCmdProcessor.getQCmdProcessor().appendToQueue(new QCmdStartFlasher());
        } else {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, "can''t read firmware, please compile firmware! (file: {0} )", fname_fw);
        }
    }

    public Boolean getSDCardMounted() {
        return sDCardMounted;
    }

    public void setSDCardMounted(Boolean SDCardMounted) {
        this.sDCardMounted = SDCardMounted;
        firePropertyChange(HAS_SDCARD,
                null, SDCardMounted);
    }

    public TargetRTInfo getRTInfo() {
        return RTInfo;
    }

    public void setRTInfo(TargetRTInfo RTInfo) {
        this.RTInfo = RTInfo;
        firePropertyChange(RTINFO,
                null, RTInfo);
    }

    public int getPatchIndex() {
        return patchIndex;
    }

    public void setPatchIndex(Integer patchIndex) {
        if (this.patchIndex == patchIndex) {
            return;
        }
        this.patchIndex = patchIndex;
        readPatchName();
        firePropertyChange(PATCHINDEX, null, this.patchIndex);
    }

    public Boolean getWarnedAboutFWCRCMismatch() {
        return warnedAboutFWCRCMismatch;
    }

    public void setWarnedAboutFWCRCMismatch(Boolean WarnedAboutFWCRCMismatch) {
        this.warnedAboutFWCRCMismatch = WarnedAboutFWCRCMismatch;
        firePropertyChange(WARNEDABOUTFWCRCMISMATCH, null, WarnedAboutFWCRCMismatch);
    }

    public SDCardInfo getSDCardInfo() {
        return sdcardInfo;
    }

    public void setSDCardInfo(SDCardInfo sDCardInfo) {
        this.sdcardInfo = sDCardInfo;
        firePropertyChange(SDCARDINFO, null, sDCardInfo);
    }

    public MidiMonitorData getMidiMonitor() {
        return midiMonitor;
    }

    public void setMidiMonitor(MidiMonitorData midiMonitor) {
        this.midiMonitor = midiMonitor;
        firePropertyChange(MIDIMONITOR, null, midiMonitor);
    }

    public void readPatchName() {
        if (connection.getFWChunks() == null) {
            setPatchName("disconnected");
            return;
        }
        ChunkData chunk_output = connection.getFWChunks().getOne(FourCCs.FW_PATCH_NAME);
        if (chunk_output == null) {
            setPatchName("???");
            return;
        }
        ByteBuffer data = chunk_output.getData();
        int addr = data.getInt();
        connection.appendToQueue(new QCmdMemRead(addr, 32, new IConnection.MemReadHandler() {
            @Override
            public void done(ByteBuffer mem) {
                if (mem == null) {
                    setPatchName("failed");
                    return;
                }
                String s = "";
                while (mem.hasRemaining()) {
                    char c = (char) mem.get();
                    if (c == 0) {
                        break;
                    }
                    s += c;
                }
                setPatchName(s);
            }
        }));
    }

    public String getPatchName() {
        return patchName;
    }

    public void setPatchName(String patchName) {
        this.patchName = patchName;
        firePropertyChange(PATCHNAME, null, patchName);
    }

    private final List<PollHandler> pollers = new LinkedList<>();

    public List<PollHandler> getPollers() {
        return Collections.unmodifiableList(pollers);
    }

    public void addPoller(PollHandler poller) {
        pollers.add(poller);
    }

    public void removePoller(PollHandler poller) {
        pollers.remove(poller);
    }

    @Override
    protected TargetController createController() {
        return new TargetController(this);
    }

    @Override
    public IModel getParent() {
        return null;
    }

}
