package sc;

/*
 * MyKad.java
 *
 * Created on April 19, 2007, 6:50 PM
 *
 */

import com.linuxnet.jpcsc.Apdu;
import com.linuxnet.jpcsc.Card;
import com.linuxnet.jpcsc.Context;
import com.linuxnet.jpcsc.PCSC;
import com.linuxnet.jpcsc.PCSCException;
import com.linuxnet.jpcsc.State;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import javax.swing.Icon;
import javax.swing.ImageIcon;

public class MyKad {
    
    private static int ALL_PROTOS = PCSC.PROTOCOL_T0|PCSC.PROTOCOL_T1;
    private static Apdu.Format format = new Apdu.Format(Apdu.HEX_COLON_HEX_FORMAT, true, false);
    
    private Context ctx;
    private Card card;
    private String[] readerNames;
    
    private int JPNSel = 0;
    
    public String ic;
    public String gender;
    public String name;
    public String oldIc;
    public String dob;
    public String birthPlace;
    public String citizenship;
    public String race;
    public String religion;
    public String address1;
    public String address2;
    public String address3;
    public String poscode;
    public String city;
    public String state;
    
    /** Creates a new instance of MyKad */
    public MyKad() {
    }
    
    public void start() {
        
        /*
         * The Context class wraps the PCSC functions related to connecting/disconnecting
         * to the PCSC service and card readers. An invocation of Connect() returns a connection
         * to a Card allowing for the transmission of APDUs.
         */
        
        ctx = new Context();
        
        try {
            
            ctx.EstablishContext(PCSC.SCOPE_SYSTEM, null, null);
            readerNames = ctx.ListReaders();
            
        } catch(Exception e){
            
            System.out.println("ERROR: " + StringValues.CANNOT_CONNECT_TO_PCSC_SERVICE);
            System.exit(0);
            
        }
        
        if (readerNames.length == 0) {
            System.out.println("ERROR: " + StringValues.NO_READERS_AVAILABLE);
            System.exit(0);
        }
        
        // Connect to the card
        if (card != null) {
            // Try to disconnect any previous card connection
            try {
                card.Disconnect(PCSC.LEAVE_CARD);
            } catch (Exception e) {
                System.out.println("ERROR: ");
            }
        }
        
        // Check reader until card is inserted
        String reader = readerNames[0];
        
        State[] rsa = new State[1];
        rsa[0] = new State(reader);
        
        do {
            
            try {
                
                ctx.GetStatusChange(1000, rsa); // return 34 if card is inserted
                
            } catch(Exception e) {
                System.out.println("ERROR: " + StringValues.CONTEXT_GETSTATUSCHANGE_FAILED + e.getMessage());
                return;
            }
            
        } while ((rsa[0].dwEventState & PCSC.STATE_PRESENT) != PCSC.STATE_PRESENT);
        
        System.out.println(StringValues.READERSTATE_OF + reader);
        System.out.println(rsa[0].toString());
        
        try {
            // Connect to a reader and return handle to card.
            card = ctx.Connect(reader, PCSC.SHARE_EXCLUSIVE, ALL_PROTOS);
        } catch (Exception e) {
            System.out.println("ERROR: " + StringValues.CARD_CONNECT_FAILED + e.getMessage());
            return;
        }
        
        if (card == null){            
            System.out.println("ERROR: " + StringValues.NO_ACTIVE_READER_CONNECTION + StringValues.ERROR);
            return;
        }
    }
    
    public void useJPN() {
        
        String cmd[] = {APDU.ATR, APDU.SELECT_JPN_APPLICATION, APDU.SELECT_APPLICATION_GET_RESPONSE};
        
        for (int i=1; i<cmd.length; i++) {
            
            Apdu apdu = new Apdu(256);
            
            try {
                
                String s = cmd[i].trim().replace(" ", "");
                
                if (s.length() == 0) throw new RuntimeException(StringValues.NO_APDU_GIVEN);
                if ((s.length() % 2) != 0) throw new RuntimeException(StringValues.ODD_LENGTH_OF + s);
                
                // Set APDU Command
                System.out.println("Command: " + s);
                apdu.set(s);
                
            } catch (Exception e) {
                System.out.println("ERROR: " + StringValues.INVALID_INPUT + e.getMessage());
                return;
            }
            
            try {
                
                byte[] response = card.Transmit(apdu);
                
                if (i == (cmd.length-1)) {
                    String feedback = Apdu.ba2s(response, format);
                }
                
            } catch (PCSCException pe) {
                System.out.println(StringValues.CARD_TRANSMIT + pe.getMessage());
                return;
                
            } catch (Exception e) {
                
                System.out.println(StringValues.CARD_TRANSMIT_FAILED + e.getMessage());
                return;
            }
        }
        
        
    }
    
    public String send(String cmd[]) {
        
        String rs     = "";
        String offset = cmd[1];
        String length = cmd[0];
        
        System.out.println("Offset: " + offset + "," + "Length: " + length);
        
        for (int i=0; i<cmd.length; i++) {
            
            Apdu apdu = new Apdu(256);
            
            try {
                
                String altered = null;
                
                if (i == 0)
                    altered = APDU.SET_LENGTH + " " + length + " 00";
                else if (i == 1)
                    altered = APDU.SELECT_INFO + " " + APDU.JPN[JPNSel] + " " + offset + " " + length + " 00";
                else if (i == 2)
                    altered = APDU.READ_INFO + " " + length;
                
                // System.out.println("Command " + i + ": " + altered);
                String s = altered.trim().replace(" ", "");
                
                if (s.length() == 0) throw new RuntimeException(StringValues.NO_APDU_GIVEN);
                if ((s.length() % 2) != 0) throw new RuntimeException(StringValues.ODD_LENGTH_OF + s);
                
                // Set APDU Command
                apdu.set(s);
                
            } catch (Exception e) {
                System.out.println(StringValues.INVALID_INPUT + e.getMessage());
                return rs;
            }
            
            try {
                
                byte[] response = card.Transmit(apdu);
                
                if (i == (cmd.length-1)) {
                    String feedback = Apdu.ba2s(response, format);
                    System.out.println("Feedback: " + feedback);
                    rs = hexToText(feedback);
                    System.out.println("Converted Feedback: " + rs);
                }
                
            } catch (PCSCException pe) {
                System.out.println(StringValues.CARD_TRANSMIT + pe.getMessage());
                return rs;
                
            } catch (Exception e) {
                
                System.out.println(StringValues.CARD_TRANSMIT_FAILED + e.getMessage());
                return rs;
            }
        }
        
        return rs;
    }
    
    public String sendNoConvert(String cmd[]) {
        
        String rs     = "";
        String offset = cmd[1];
        String length = cmd[0];
        
        // System.out.println("Offset: " + offset + "," + "Length: " + length);
        
        for (int i=0; i<cmd.length; i++) {
            
            Apdu apdu = new Apdu(256);
            
            try {
                
                String altered = null;
                
                if (i == 0)
                    altered = APDU.SET_LENGTH + " " + length + " 00";
                else if (i == 1)
                    altered = APDU.SELECT_INFO + " " + APDU.JPN[JPNSel] + " " + offset + " " + length + " 00";
                else if (i == 2)
                    altered = APDU.READ_INFO + " " + length;
                
                // System.out.println("Command " + i + ": " + altered);
                String s = altered.trim().replace(" ", "");
                
                if (s.length() == 0) throw new RuntimeException(StringValues.NO_APDU_GIVEN);
                if ((s.length() % 2) != 0) throw new RuntimeException(StringValues.ODD_LENGTH_OF + s);
                
                // Set APDU Command
                apdu.set(s);
                
            } catch (Exception e) {
                System.out.println(StringValues.INVALID_INPUT + e.getMessage());
                return rs;
            }
            
            try {
                
                byte[] response = card.Transmit(apdu);
                
                if (i == (cmd.length-1)) {
                    String feedback = Apdu.ba2s(response, format);
                    rs = feedback.substring(3, feedback.length()).replace(":90:00:", "");
                    // System.out.println("Feedback: " + feedback);
                }
                
            } catch (PCSCException pe) {
                System.out.println(StringValues.CARD_TRANSMIT + pe.getMessage());
                return rs;
                
            } catch (Exception e) {
                
                System.out.println(StringValues.CARD_TRANSMIT_FAILED + e.getMessage());
                return rs;
            }
        }
        
        return rs;
    }
    
    public void stop() {
        ctx.Cancel();
        
        if (card != null) {
            card.Disconnect();
        }
    }
    
    public String hexToText(String str) {
        
        String newStr = "";
        
        try {
            
            String str2[] = str.split(" ");
            
            // Delete useless things
            str = str2[1].replace("90:00:", "");
            
            String tmp[] = str.split(":");
            
            for (int i=0; i<tmp.length; i++) {
                String converted = String.valueOf((char) Integer.parseInt(tmp[i].trim(), 16));
                newStr += converted;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        
        return newStr.trim();
    }
    
    public Icon getPic() {
        
        Icon icon = null;
        
        Apdu apdu  = new Apdu(256);
        String rs  = null;
        int offset = 0x03;
        int length = 0xff;
        int max    = 4000;
        
        JPNSel = 1;
        
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        
        boolean go = true;
        do {
            
            String tmp = Integer.toHexString(offset);
            
            String tmpOffset = null;
            
            if (tmp.length() == 1) {
                tmpOffset = "0" + tmp + " 00";
            } else {
                tmpOffset = tmp.substring(1,3) + " 0" + tmp.substring(0,1);
            }
            
            String cmd[] = {APDU.SET_LENGTH + "FF 00",
            APDU.SELECT_INFO +  APDU.JPN[JPNSel] + tmpOffset + "FF 00",
            APDU.READ_INFO + "FF"};
            
            for (int i=0; i<cmd.length; i++) {
                
                try {
                    
                    String s = cmd[i].trim().replace(" ", "");
                    
                    if (s.length() == 0) throw new RuntimeException(StringValues.NO_APDU_GIVEN);
                    if ((s.length() % 2) != 0) throw new RuntimeException(StringValues.ODD_LENGTH_OF + s);
                    
                    // Set APDU Command
                    apdu.set(s);
                    
                } catch (Exception e) {
                    System.out.println("ERROR: " + StringValues.INVALID_INPUT + e.getMessage());
                    return icon;
                }
                
                try {
                    
                    byte[] response = card.Transmit(apdu);
                    
                    if (i == 2) {
                        String tmpp = Apdu.ba2s(response);
                        String arr[] = tmpp.split(" ");
                        arr[1] = arr[1].replaceAll("9000", "");
                        System.out.println(arr[1]);
                        
                        if (!arr[1].equalsIgnoreCase("9607"))
                            output.write(toBinArray(arr[1]));
                        else
                            go = false;
                    }
                    
                } catch (PCSCException pe) {
                    System.out.println("ERROR: " + StringValues.CARD_TRANSMIT + pe.getMessage());
                    return icon;
                    
                } catch (Exception e) {
                    System.out.println("ERROR: " + StringValues.CARD_TRANSMIT_FAILED + e.getMessage());
                    return icon;
                }
            }
            
            if ((offset + length) > max)
                offset += (max - offset);
            else
                offset += length;            
            
        } while (go);
        
        Image image = Toolkit.getDefaultToolkit().createImage(output.toByteArray());
        icon = new ImageIcon(image);
        
        return icon;
    }
    
    public void getThumbPrint() {
        
        String offset = null;
        
        offset = "17 00";
        
        JPNSel = 2;
        
        Apdu apdu  = new Apdu(256);
        
        String thumbprint[] = { APDU.THUMBPRINT_RIGHT[1], APDU.THUMBPRINT_LEFT[1]};
        
        for (int m=0; m<thumbprint.length; m++) {
            
            String cmd[] = {APDU.SET_LENGTH + "FF 00",
            APDU.SELECT_INFO +  APDU.JPN[JPNSel] + thumbprint[m] + "FF 00",
            APDU.READ_INFO + "FF"};
            
            for (int i=0; i<cmd.length; i++) {
                
                try {
                    
                    String s = cmd[i].trim().replace(" ", "");;
                    
                    if (s.length() == 0) throw new RuntimeException(StringValues.NO_APDU_GIVEN);
                    if ((s.length() % 2) != 0) throw new RuntimeException(StringValues.ODD_LENGTH_OF + s);
                    
                    // Set APDU Command
                    System.out.println("Command: " + s);
                    apdu.set(s);
                    
                } catch (Exception e) {
                    System.out.println("ERROR: " + StringValues.INVALID_INPUT + e.getMessage());
                    return;
                }
                
                try {
                    
                    byte[] response = card.Transmit(apdu);
                    
                    if (i == 2) {
                        String feedback = Apdu.ba2s(response, format);
                    }
                    
                } catch (PCSCException pe) {
                    System.out.println(StringValues.CARD_TRANSMIT + pe.getMessage());
                    return;
                    
                } catch (Exception e) {
                    System.out.println(StringValues.CARD_TRANSMIT_FAILED + e.getMessage());
                    return;
                }
            }
        }
        
    }
    
    
    public byte[] toBinArray( String hexStr ){
        byte bArray[] = new byte[hexStr.length()/2];
        for(int i=0; i<(hexStr.length()/2); i++){
            byte firstNibble  = Byte.parseByte(hexStr.substring(2*i,2*i+1),16); // [x,y)
            byte secondNibble = Byte.parseByte(hexStr.substring(2*i+1,2*i+2),16);
            int finalByte = (secondNibble) | (firstNibble << 4 ); // bit-operations only with numbers, not bytes.
            bArray[i] = (byte) finalByte;
        }
        return bArray;
    }
    
    
    public void readData() {
        
        String str = null;
        
        if (card != null) {
            
            String nameCmd[] = {APDU.NAME[0], APDU.NAME[1], ""};
            name = send(nameCmd).replaceAll("[ ]{2,}", " ");
            
            String icCmd[] = {APDU.IC_NUMBER[0], APDU.IC_NUMBER[1], ""};
            ic = send(icCmd);
            
            String genderCmd[] = {APDU.GENDER[0], APDU.GENDER[1], ""};
            gender = send(genderCmd);
            
            String oldIcCmd[] = {APDU.OLD_IC[0], APDU.OLD_IC[1], ""};
            oldIc = send(oldIcCmd);
            
            String dobCmd[] = {APDU.DOB[0], APDU.DOB[1], ""};
            dob = sendNoConvert(dobCmd).replaceFirst(":", "").replaceAll(":", "-");
            
            String birthPlaceCmd[] = {APDU.BIRTH_PLACE[0], APDU.BIRTH_PLACE[1], ""};
            birthPlace = send(birthPlaceCmd);
            
            String citizenshipCmd[] = {APDU.CITIZENSHIP[0], APDU.CITIZENSHIP[1], ""};
            citizenship = send(citizenshipCmd);
            
            String raceCmd[] = {APDU.RACE[0], APDU.RACE[1], ""};
            race = send(raceCmd);
            
            String religionCmd[] = {APDU.RELIGION[0], APDU.RELIGION[1], ""};
            religion = send(religionCmd);
            
            // Change to jpn-1-4
            JPNSel = 3;
            
            String address1Cmd[] = {APDU.ADDRESS_1[0], APDU.ADDRESS_1[1], ""};
            address1 = send(address1Cmd);
            
            String address2Cmd[] = {APDU.ADDRESS_2[0], APDU.ADDRESS_2[1], ""};
            address2 = send(address2Cmd);
            
            String address3Cmd[] = {APDU.ADDRESS_3[0], APDU.ADDRESS_3[1], ""};
            address3 = send(address3Cmd);
            
            String poscodeCmd[] = {APDU.POSCODE[0], APDU.POSCODE[1], ""};
            poscode = sendNoConvert(poscodeCmd).replaceAll(":", "").substring(0,5);
            poscode = "";
            
            String cityCmd[] = {APDU.CITY[0], APDU.CITY[1], ""};
            city = send(cityCmd);
            
            String stateCmd[] = {APDU.STATE[0], APDU.STATE[1], ""};
            state = send(stateCmd);
        }
        
    }
}

class APDU {
    
    static final String NAME[]                              = { "28", "E9 00" };
    static final String IC_NUMBER[]                         = { "0D", "11 01" };
    static final String GENDER[]                            = { "01", "1E 01" };
    static final String OLD_IC[]                            = { "08", "1F 01" };
    static final String DOB[]                               = { "04", "27 01" };
    static final String BIRTH_PLACE[]                       = { "19", "2B 01" };
    static final String CITIZENSHIP[]                       = { "12", "48 01" };
    static final String RACE[]                              = { "19", "5A 01" };
    static final String RELIGION[]                          = { "0B", "73 01" };
    
    static final String ADDRESS_1[]                         = { "1E", "03 00" };
    static final String ADDRESS_2[]                         = { "1E", "21 00" };
    static final String ADDRESS_3[]                         = { "1E", "3F 00" };
    static final String POSCODE[]                           = { "03", "5D 00" };
    static final String CITY[]                              = { "19", "60 00" };
    static final String STATE[]                             = { "1E", "79 00" };
    
    static final String THUMBPRINT_RIGHT[]                  = { "FF", "17 00" };
    static final String THUMBPRINT_LEFT[]                   = { "FF", "6D 02" };
    
    static final String ATR                                 = "3B 67 00 00 73 20 00 6C 68 90 00";
    static final String SELECT_JPN_APPLICATION              = "00 A4 04 00 0A A0 00 00 00 74 4A 50 4E 00 10";
    static final String SELECT_APPLICATION_GET_RESPONSE     = "00 C0 00 00 05";
    
    static final String SET_LENGTH                          = "C8 32 00 00 05 08 00 00";
    static final String SELECT_INFO                         = "CC 00 00 00 08";
    static final String READ_INFO                           = "CC 06 00 00";
    
    static final String JPN[]                               = { "01 00 01 00", "02 00 01 00", "03 00 01 00",
                                                                "04 00 01 00", "05 00 01 00", "06 00 01 00"};
}

/*
 * StringValues.java
 *
 * Created on 2 luglio 2004, 11.14
 */

/**
 *
 * @author  ue_sergi
 */
class StringValues {
    
    static final String  APDU_TEXT                         = "80CA9F7F00" ;
    static final String  APPLET                            = "A0000000030000";
    static final String  APDU_SENDER                       = "Apdu Sender "   ;
    static final String  APDU                              = "APDU:         ";
    static final String  APPLET_AID                        = "Applet AID:";
    static final String  APPLET_SELECTOR                   = "Applet Selector ";
    static final String  CANNOT_CONNECT_TO_PCSC_SERVICE    = "Cannot connect to PCSC service!";
    static final String  CARD_CONNECT_FAILED               = "Card.Connect() failed!\n";
    static final String  CARD_TRANSMIT_FAILED              = "Card.Transmit() failed!\n";
    static final String  CARD_TRANSMIT                     = "Card.Transmit(): ";
    static final String  CONNECT                           = "Connect...";
    static final String  CONNECTING                        = "Connecting...";
    static final String  CONTEXT_GETSTATUSCHANGE_FAILED    = "Context.GetStatusChange() failed!\n";
    static final String  DO_NOT_FORGET_TO_INSERT_A_CARD    = "\nDo not forget to insert a card!";
    static final String  ERROR_CLASS                       = "Error class: ";
    static final String  ERROR_MESSAGE                     = "Error message: ";
    static final String  ERROR                             = "Error";
    static final String  INTERNAL_ERROR                    = "Internal error";
    static final String  INVALID_INPUT                     = "Invalid input: ";
    static final String  NO_ACTIVE_READER_CONNECTION       = "No active reader connection!";
    static final String  NO_AID_GIVEN                      = "No AID given";
    static final String  NO_APDU_GIVEN                     = "No APDU given";
    static final String  NO_READERS_AVAILABLE              = "No readers available!";
    static final String  ODD_LENGTH_OF                     = "Odd length of ";
    static final String  OUTPUT                            = "Output ";
    static final String  PCSC_ERROR_MESSAGE                = "PCSC Error Message: ";
    static final String  QUIT                              = "Quit";
    static final String  READER_SELECTOR                   = "Reader Selector ";
    static final String  READER                            = "Reader:      ";
    static final String  READERSTATE_OF                    = "ReaderState of ";
    static final String  RECEIVED                          = "Received ";
    static final String  SELECT                            = "Select... ";
    static final String  SELECTING_APPLET                  = "Selecting applet ";
    static final String  SEND                              = "Send...   ";
    static final String  SENDING                           = "Sending ";
    static final String  TRYING_TO_CONNECT_TO_READER       = "Trying to connect to reader ";
}

