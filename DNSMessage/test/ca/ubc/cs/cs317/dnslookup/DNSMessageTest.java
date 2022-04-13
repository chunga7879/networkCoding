package ca.ubc.cs.cs317.dnslookup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DNSMessageTest {
    @Test
    public void testConstructor() {
        DNSMessage message = new DNSMessage((short)23);
        assertFalse(message.getQR());
        assertFalse(message.getRD());
        assertEquals(0, message.getQDCount());
        assertEquals(0, message.getANCount());
        assertEquals(0, message.getNSCount());
        assertEquals(0, message.getARCount());
        assertEquals(23, message.getID());
    }
    @Test
    public void testBasicFieldAccess() {
        DNSMessage message = new DNSMessage((short)23);
        message.setOpcode(0);
        message.setQR(true);
        message.setRD(true);
        message.setQDCount(1);
        assertTrue(message.getQR());
        assertTrue(message.getRD());
        assertEquals(1, message.getQDCount());
    }
    @Test
    public void testAddQuestion() {
        DNSMessage request = new DNSMessage((short)23);
        DNSQuestion question = new DNSQuestion("norm.cs.ubc.ca", RecordType.A, RecordClass.IN);
        request.addQuestion(question);
        byte[] content = request.getUsed();

        DNSMessage reply = new DNSMessage(content, content.length);
        assertEquals(request.getID(), reply.getID());
        assertEquals(request.getQDCount(), reply.getQDCount());
        assertEquals(request.getANCount(), reply.getANCount());
        assertEquals(request.getNSCount(), reply.getNSCount());
        assertEquals(request.getARCount(), reply.getARCount());
        DNSQuestion replyQuestion = reply.getQuestion();
        assertEquals(question, replyQuestion);
    }
    @Test
    public void testAddResourceRecord() {
        DNSMessage request = new DNSMessage((short)23);
        DNSQuestion question = new DNSQuestion("norm.cs.ubc.ca", RecordType.NS, RecordClass.IN);
        ResourceRecord rr = new ResourceRecord(question, RecordType.NS.getCode(), "ns1.cs.ubc.ca");
        request.addResourceRecord(rr);
        byte[] content = request.getUsed();

        DNSMessage reply = new DNSMessage(content, content.length);
        assertEquals(request.getID(), reply.getID());
        assertEquals(request.getQDCount(), reply.getQDCount());
        assertEquals(request.getANCount(), reply.getANCount());
        assertEquals(request.getNSCount(), reply.getNSCount());
        assertEquals(request.getARCount(), reply.getARCount());
        ResourceRecord replyRR = reply.getRR();
        assertEquals(rr, replyRR);
    }

    @Test
    public void testAddResourceRecordMX() {
        DNSMessage request = new DNSMessage((short)23);
        DNSQuestion question = new DNSQuestion("norm.cs.ubc.ca", RecordType.MX, RecordClass.IN);
        ResourceRecord rr = new ResourceRecord(question, RecordType.MX.getCode(), "mail.ubc.ca");
        request.addResourceRecord(rr);
        byte[] content = request.getUsed();

        DNSMessage reply = new DNSMessage(content, content.length);
        assertEquals(request.getID(), reply.getID());
        assertEquals(request.getQDCount(), reply.getQDCount());
        assertEquals(request.getANCount(), reply.getANCount());
        assertEquals(request.getNSCount(), reply.getNSCount());
        assertEquals(request.getARCount(), reply.getARCount());
        ResourceRecord replyRR = reply.getRR();
        assertEquals(rr, replyRR);
    }

    @Test
    public void testSetID() {
        DNSMessage request = new DNSMessage((short)23);
        DNSQuestion question = new DNSQuestion("norm.cs.ubc.ca", RecordType.A, RecordClass.IN);
        request.setID((short)50);
        request.addQuestion(question);
        assertEquals(50, request.getID());
    }

    @Test
    public void testSetQR() {
        DNSMessage request = new DNSMessage((short)23);
        DNSQuestion question = new DNSQuestion("norm.cs.ubc.ca", RecordType.A, RecordClass.IN);
        request.setQR(false);
        request.addQuestion(question);
        assertEquals(false, request.getQR());
    }

    @Test
    public void testSetOpCode() {
        DNSMessage request = new DNSMessage((short)23);
        DNSQuestion question = new DNSQuestion("norm.cs.ubc.ca", RecordType.A, RecordClass.IN);
        request.setOpcode(15);
        request.addQuestion(question);
        assertEquals(15, request.getOpcode());
    }
//
//    @Test
//    public void testSet() {
//        DNSMessage request = new DNSMessage((short)23);
//        DNSQuestion question = new DNSQuestion("norm.cs.ubc.ca", RecordType.A, RecordClass.IN);
//        request.setID((short)50);
//        request.addQuestion(question);
//        assertEquals(50, request.getID());
//    }
}
