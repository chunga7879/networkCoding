package ca.ubc.cs.cs317.dnslookup;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class DNSMessage {
    public static final int MAX_DNS_MESSAGE_LENGTH = 512;
    public static final int QUERY = 0;
    /**
     * TODO:  You will add additional constants and fields
     */
    private final Map<String, Integer> nameToPosition = new HashMap<>();
    private final Map<Integer, String> positionToName = new HashMap<>();
    private final ByteBuffer buffer;

    private int id; // 16bit(2byte)
    private boolean qr = false; // True: 1 (Response) False: 0 (Query) 1bit
    private int opCode = 0; // 4bit
    private boolean aa = false; // True: 1 False: 0  1bit
    private boolean tc = false; // True: 1 False: 0  1bit
    private boolean rd = false; // True: 1 False: 0  1bit
    private boolean ra = false; // True: 1 False: 0  1bit
    private int rCode = 0; // 4bit
    private int z = 0; // 3bit
    private int qdCount = 0; // 16bit(2byte)
    private int anCount = 0; // 16bit(2byte)
    private int nsCount = 0; // 16bit(2byte)
    private int arCount = 0; // 16bit(2byte)


    /**
     * Initializes an empty DNSMessage with the given id.
     *
     * @param id The id of the message.
     */
    public DNSMessage(short id) {
        this.buffer = ByteBuffer.allocate(MAX_DNS_MESSAGE_LENGTH);
        // TODO: Complete this method
        this.id = id & 0xffff;
        short defaultSecondLine = 0b0000000000000000;
        buffer.putShort(id);
        buffer.putShort(defaultSecondLine);
        buffer.putShort((short)qdCount);
        buffer.putShort((short)anCount);
        buffer.putShort((short)nsCount);
        buffer.putShort((short)arCount);
    }

    /**
     * Initializes a DNSMessage with the first length bytes of the given byte array.
     *
     * @param recvd The byte array containing the received message
     * @param length The length of the data in the array
     */
    public DNSMessage(byte[] recvd, int length) {
        buffer = ByteBuffer.wrap(recvd, 0, length);
        // TODO: Complete this method
        getID(); // 16bit(2byte)
        getQR(); // True: 1 (Response) False: 0 (Query) 1bit
        getOpcode(); // 4bit
        getAA(); // True: 1 False: 0  1bit
        getTC(); // True: 1 False: 0  1bit
        getRD(); // True: 1 False: 0  1bit
        getRA(); // True: 1 False: 0  1bit
        getRcode(); // 4bit
        getQDCount(); // 16bit(2byte)
        getANCount(); // 16bit(2byte)
        getNSCount(); // 16bit(2byte)
        getARCount(); // 16bit(2byte)
        buffer.position(12);
    }

    /**
     * Getters and setters for the various fixed size and fixed location fields of a DNSMessage
     * TODO:  They are all to be completed
     */
    public int getID() {
        int idChange = buffer.getShort(0);
        this.id = idChange & 0xffff;
        return this.id;
    }

    // 2byte
    public void setID(int id) {
        buffer.putShort(0, (short)id);
        this.id = id;
    }

    public boolean getQR() {
        short prev = buffer.getShort(2);
        int qr = prev & 0b1000000000000000;
        if (qr == 0b1000000000000000) {
            this.qr = true;
        } else {
            this.qr = false;
        }
        return this.qr;

    }

    public void setQR(boolean qr) {
        short prev = buffer.getShort(2);
        int prevWithout = 0b0111111111111111 & prev;
        int qrAtPosition;

        if (qr) {
            qrAtPosition = 0b1 << 15;
        } else {
            qrAtPosition = 0;
        }

        int current = qrAtPosition | prevWithout;
        buffer.putShort(2, (short)current);
        this.qr = qr;
    }

    public boolean getAA() {
        short prev = buffer.getShort(2);
        int aa = prev & 0b0000010000000000;
        if (aa == 0b0000010000000000) {
            this.aa = true;
        } else {
            this.aa = false;
        }
        return this.aa;

    }

    public void setAA(boolean aa) {
        int prev = buffer.getShort(2);
        int prevWithout = 0b1111101111111111 & prev;
        int aaAtPosition;
        if (aa) {
            aaAtPosition = 0b1 << 10;
        } else {
            aaAtPosition = 0b0;
        }
        int current = prevWithout | aaAtPosition;
        buffer.putShort(2, (short) current);
        this.aa = aa;
    }

    public int getOpcode() {
        int prev = buffer.getShort(2);
        int opCodePart = 0b0111100000000000 & prev;
        this.opCode = opCodePart >> 11;
        return this.opCode;
    }

    public void setOpcode(int opcode) {
        // 0(4bit opcode)000
        int prev = buffer.getShort(2);
        int prevWithout = 0b1000011111111111 & prev;
        int opcodeAtPosition = opcode << 11;
        int current = prevWithout | opcodeAtPosition;
        buffer.putShort(2, (short)current);
        this.opCode = opcode;
    }

    public boolean getTC() {
        short prev = buffer.getShort(2);
        int tc = prev & 0b0000001000000000;
        if (tc == 0b0000001000000000) {
            this.tc = true;
        } else {
            this.tc = false;
        }
        return this.tc;

    }

    public void setTC(boolean tc) {
        int prev = buffer.getShort(2);
        int prevWithout = 0b1111110111111111 & prev;
        int tcAtPosition;
        if (tc) {
            tcAtPosition = 0b1 << 9;
        } else {
            tcAtPosition = 0b0;
        }
        int current = prevWithout | tcAtPosition;
        buffer.putShort(2, (short) current);
        this.tc = tc;
    }

    public boolean getRD() {
        short prev = buffer.getShort(2);
        int rd = prev & 0b0000000100000000;
        if (rd == 0b0000000100000000) {
            this.rd = true;
        } else {
            this.rd = false;
        }
        return this.rd;

    }

    public void setRD(boolean rd) {
        int prev = buffer.getShort(2);
        int prevWithout = 0b1111111011111111 & prev;
        int rdAtPosition;
        if (rd) {
            rdAtPosition = 0b1 << 8;
        } else {
            rdAtPosition = 0b0;
        }
        int current = prevWithout | rdAtPosition;
        buffer.putShort(2, (short) current);
        this.rd = rd;
    }

    public boolean getRA() {
        short prev = buffer.getShort(2);
        int ra = prev & 0b0000000010000000;
        if (ra == 0b0000000010000000) {
            this.ra = true;
        } else {
            this.ra = false;
        }
        return this.ra;
    }

    public void setRA(boolean ra) {
        int prev = buffer.getShort(2);
        int prevWithout = 0b1111111101111111 & prev;
        int aaAtPosition;
        if (ra) {
            aaAtPosition = 0b1 << 7;
        } else {
            aaAtPosition = 0b0;
        }
        int current = prevWithout | aaAtPosition;
        buffer.putShort(2, (short) current);
        this.ra = ra;
    }

    public int getRcode() {
        int prev = buffer.getShort(2);
        this.rCode = 0b0000000000001111 & prev;
        return this.rCode;
    }

    public void setRcode(int rcode) {
        int prev = buffer.getShort(2);
        int prevWithout = 0b1111111111110000 & prev;
        int current = prevWithout | rcode;
        buffer.putShort(2, (short)current);
        this.rCode = rcode;
    }

    public int getQDCount() {
        this.qdCount = buffer.getShort(4) & 0xffff;
        return this.qdCount;
    }

    public void setQDCount(int count) {
        buffer.putShort(4, (short)count);
        this.qdCount = count;
    }

    public int getANCount() {
        this.anCount = buffer.getShort(6) & 0xffff;
        return this.anCount;
    }

    public int getNSCount() {
        this.nsCount = buffer.getShort(8) & 0xffff;
        return this.nsCount;
    }

    public int getARCount() {
        this.arCount = buffer.getShort(10) & 0xffff;
        return this.arCount;
    }

    public void setARCount(int count) {
        buffer.putShort(10, (short)count);
        this.arCount = count;
    }

    /**
     * Return the name at the current position() of the buffer.  This method is provided for you,
     * but you should ensure that you understand what it does and how it does it.
     *
     * The trick is to keep track of all the positions in the message that contain names, since
     * they can be the target of a pointer.  We do this by storing the mapping of position to
     * name in the positionToName map.
     *
     * @return The decoded name
     */
    public String getName() {
        // Remember the starting position for updating the name cache
        int start = buffer.position();
        int len = buffer.get() & 0xff;
        if (len == 0) return "";
        if ((len & 0xc0) == 0xc0) {  // This is a pointer
            int pointer = ((len & 0x3f) << 8) | (buffer.get() & 0xff);
            String suffix = positionToName.get(pointer);
            assert suffix != null;
            positionToName.put(start, suffix);
            return suffix;
        }
        byte[] bytes = new byte[len];
        buffer.get(bytes, 0, len);
        String label = new String(bytes, StandardCharsets.UTF_8);
        String suffix = getName();
        String answer = suffix.isEmpty() ? label : label + "." + suffix;
        positionToName.put(start, answer);
        return answer;
    }

    /**
     * The standard toString method that displays everything in a message.
     * @return The string representation of the message
     */
    public String toString() {
        // Remember the current position of the buffer so we can put it back
        // Since toString() can be called by the debugger, we want to be careful to not change
        // the position in the buffer.  We remember what it was and put it back when we are done.
        int end = buffer.position();
        final int DataOffset = 12;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("ID: ").append(getID()).append(' ');
            sb.append("QR: ").append(getQR()).append(' ');
            sb.append("OP: ").append(getOpcode()).append(' ');
            sb.append("AA: ").append(getAA()).append('\n');
            sb.append("TC: ").append(getTC()).append(' ');
            sb.append("RD: ").append(getRD()).append(' ');
            sb.append("RA: ").append(getRA()).append(' ');
            sb.append("RCODE: ").append(getRcode()).append(' ')
                    .append(dnsErrorMessage(getRcode())).append('\n');
            sb.append("QDCount: ").append(getQDCount()).append(' ');
            sb.append("ANCount: ").append(getANCount()).append(' ');
            sb.append("NSCount: ").append(getNSCount()).append(' ');
            sb.append("ARCount: ").append(getARCount()).append('\n');
            buffer.position(DataOffset);
            showQuestions(getQDCount(), sb);
            showRRs("Authoritative", getANCount(), sb);
            showRRs("Name servers", getNSCount(), sb);
            showRRs("Additional", getARCount(), sb);
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "toString failed on DNSMessage";
        }
        finally {
            buffer.position(end);
        }
    }

    /**
     * Add the text representation of all the questions (there are nq of them) to the StringBuilder sb.
     *
     * @param nq Number of questions
     * @param sb Collects the string representations
     */
    private void showQuestions(int nq, StringBuilder sb) {
        sb.append("Question [").append(nq).append("]\n");
        for (int i = 0; i < nq; i++) {
            DNSQuestion question = getQuestion();
            sb.append('[').append(i).append(']').append(' ').append(question).append('\n');
        }
    }

    /**
     * Add the text representation of all the resource records (there are nrrs of them) to the StringBuilder sb.
     *
     * @param kind Label used to kind of resource record (which section are we looking at)
     * @param nrrs Number of resource records
     * @param sb Collects the string representations
     */
    private void showRRs(String kind, int nrrs, StringBuilder sb) {
        sb.append(kind).append(" [").append(nrrs).append("]\n");
        for (int i = 0; i < nrrs; i++) {
            ResourceRecord rr = getRR();
            sb.append('[').append(i).append(']').append(' ').append(rr).append('\n');
        }
    }

    /**
     * Decode and return the question that appears next in the message.  The current position in the
     * buffer indicates where the question starts.
     *
     * @return The decoded question
     */
    public DNSQuestion getQuestion() {
        // TODO: Complete this method
        String hostName = getName();
        short typeCode = buffer.getShort();
        int typeCodeInt = typeCode & 0xffff;
        RecordType recordType = RecordType.getByCode(typeCodeInt);

        short classType = buffer.getShort();
        int classTypeInt = classType & 0xffff;
        RecordClass recordClass = RecordClass.getByCode(classTypeInt);

        DNSQuestion question = new DNSQuestion(hostName, recordType, recordClass);

        return question;
    }

    /**
     * Decode and return the resource record that appears next in the message.  The current
     * position in the buffer indicates where the resource record starts.
     *
     * @return The decoded resource record
     */
    public ResourceRecord getRR() {
        DNSQuestion question = getQuestion();
        int ttl = buffer.getInt();
        short len = buffer.getShort();

        ResourceRecord resourceRecord;


        try {
            if (question.getRecordType() == RecordType.A || question.getRecordType() == RecordType.AAAA) {
                byte[] rdata = new byte[len];
                for (int i = 0; i < len; i++) {
                    rdata[i] = buffer.get();
                }
                InetAddress inetResult = InetAddress.getByAddress(rdata);
                resourceRecord = new ResourceRecord(question, ttl, inetResult);

            } else if (question.getRecordType() == RecordType.MX) {
                int pref = buffer.getShort();
                String data = getName();
                resourceRecord = new ResourceRecord(question,ttl, data);
            } else {
                String data = getName();
                resourceRecord = new ResourceRecord(question,ttl, data);
            }

            return resourceRecord;

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Helper function that returns a hex string representation of a byte array. May be used to represent the result of
     * records that are returned by a server but are not supported by the application (e.g., SOA records).
     *
     * @param data a byte array containing the record data.
     * @return A string containing the hex value of every byte in the data.
     */
    public static String byteArrayToHexString(byte[] data) {
        return IntStream.range(0, data.length).mapToObj(i -> String.format("%02x", data[i])).reduce("", String::concat);
    }

    /**
     * Add an encoded name to the message. It is added at the current position and uses compression
     * as much as possible.  Compression is accomplished by remembering the position of every added
     * label.
     *
     * @param name The name to be added
     */
    public void addName(String name) {
        String label;
        while (name.length() > 0) {
            Integer offset = nameToPosition.get(name);
            if (offset != null) {
                int pointer = offset;
                pointer |= 0xc000;
                buffer.putShort((short)pointer);
                return;
            } else {
                nameToPosition.put(name, buffer.position());
                int dot = name.indexOf('.');
                label = (dot > 0) ? name.substring(0, dot) : name;
                buffer.put((byte)label.length());
                for (int j = 0; j < label.length(); j++) {
                    buffer.put((byte)label.charAt(j));
                }
                name = (dot > 0) ? name.substring(dot + 1) : "";
            }
        }
        buffer.put((byte)0);
    }

    /**
     * Add an encoded question to the message at the current position.
     * @param question The question to be added
     */
    public void addQuestion(DNSQuestion question) {
        addName(question.getHostName());
        addQType(question.getRecordType());
        addQClass(question.getRecordClass());
        qdCount++;
        setQDCount(qdCount);
    }

    /**
     * Add an encoded resource record to the message at the current position.
     * @param rr The resource record to be added
     * @param section A string describing the section that the rr should be added to
     */
    public void addResourceRecord(ResourceRecord rr, String section) {
        addName(rr.getHostName());
        addQType(rr.getRecordType());
        addQClass(rr.getRecordClass());
        buffer.putInt((int)rr.getRemainingTTL());
        byte[] data;
        if (rr.getRecordType() == RecordType.A) {
            buffer.putShort((short)0x0004);
            InetAddress inetAddress = rr.getInetResult();
            data = inetAddress.getAddress();
            buffer.put(data);

        } else if (rr.getRecordType() == RecordType.AAAA) {
            buffer.putShort((short)0x0010);
            InetAddress inetAddress = rr.getInetResult();
            data = inetAddress.getAddress();
            buffer.put(data);

        } else if (rr.getRecordType()  == RecordType.MX) {
            String textResult = rr.getTextResult();
            buffer.putShort((short) (textResult.length() +  2));
            buffer.putShort((short)0);
            addName(textResult);
        } else {
            String textResult = rr.getTextResult();
            buffer.putShort((short)textResult.length());
            addName(textResult);
        }
        if (section.equals("answer")) {
            anCount++;
        } else if (section.equals("nameserver")) {
            nsCount++;
        } else if (section.equals("additional")) {
            arCount++;
            setARCount(arCount);
        }

    }

    /**
     * Add an encoded type to the message at the current position.
     * @param recordType The type to be added
     */
    private void addQType(RecordType recordType) {
        int code = recordType.getCode();
        buffer.putShort((short)code);
    }

    /**
     * Add an encoded class to the message at the current position.
     * @param recordClass The class to be added
     */
    private void addQClass(RecordClass recordClass) {
        int code = recordClass.getCode();
        buffer.putShort((short)code);
    }

    /**
     * Return a byte array that contains all the data comprising this message.  The length of the
     * array will be exactly the same as the current position in the buffer.
     * @return A byte array containing this message's data
     */
    public byte[] getUsed() {
        // TODO: Complete this method
        int len = buffer.position();
        byte[] allData = new byte[len];

        for (int i = 0; i < len; i++) {
            allData[i] = buffer.get(i);
        }

        return allData;
    }

    /**
     * Returns a string representation of a DNS error code.
     *
     * @param error The error code received from the server.
     * @return A string representation of the error code.
     */
    public static String dnsErrorMessage(int error) {
        final String[] errors = new String[]{
                "No error", // 0
                "Format error", // 1
                "Server failure", // 2
                "Name error (name does not exist)", // 3
                "Not implemented (parameters not supported)", // 4
                "Refused" // 5
        };
        if (error >= 0 && error < errors.length)
            return errors[error];
        return "Invalid error message";
    }
}
