package org.example;
import org.example.util.ImageUtils;
import javax.smartcardio.*;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;

public class CardService {
    private CardChannel channel;
    private Card card;
    private static final byte[] APPLET_AID = {
            (byte)0x11, (byte)0x22, (byte)0x33, (byte)0x44, (byte)0x55, (byte)0x00
    };

    // --- INS COMMANDS ---
    private static final byte INS_SETUP_CARD        = (byte) 0xA0;
    private static final byte INS_IMPORT_HOST_KEY   = (byte) 0xA1;

    private static final byte INS_GET_STATUS        = (byte) 0x10;
    private static final byte INS_VERIFY_PIN        = (byte) 0x20;
    private static final byte INS_CHANGE_PIN        = (byte) 0x21;
    private static final byte INS_UNBLOCK_PIN       = (byte) 0x22;

    private static final byte INS_GET_BALANCE       = (byte) 0x30;
    private static final byte INS_TOP_UP            = (byte) 0x31;
    private static final byte INS_PAY               = (byte) 0x32;

    private static final byte INS_SET_USER_INFO     = (byte) 0x40;
    private static final byte INS_GET_USER_INFO     = (byte) 0x41;
    private static final byte INS_UPLOAD_IMAGE      = (byte) 0x50;
    private static final byte INS_DOWNLOAD_IMAGE    = (byte) 0x51;

    private static final byte INS_GET_CARD_ID       = (byte) 0x60;
    private static final byte INS_GET_CARD_PUB_KEY  = (byte) 0x61;
    private static final byte INS_AUTHENTICATE_CARD = (byte) 0x62;

    // Constants
    private static final int CHUNK_SIZE = 240;
    private static final int MAX_PIN_SIZE = 6;
    // Status Words
    public static final int SW_SUCCESS = 0x9000;
    public static final int SW_CARD_BLOCKED = 0x6983;
    public static final int SW_CARD_NOT_SETUP = 0x6901;
    public static final int SW_UNKNOWN = 0x6F00;

    public boolean connect() {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> terminals = factory.terminals().list();
            if (terminals.isEmpty()) return false;

            CardTerminal terminal = terminals.get(0);
            System.out.println("Kết nối tới: " + terminal.getName());
            card = terminal.connect("*");
            channel = card.getBasicChannel();

            CommandAPDU select = new CommandAPDU(0x00, 0xA4, 0x04, 0x00, APPLET_AID);
            ResponseAPDU resp = channel.transmit(select);
            return resp.getSW() == SW_SUCCESS;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void disconnect() {
        try {
            if (card != null) {
                card.disconnect(false);
            }
        } catch (Exception e) {
            System.err.println("Lỗi ngắt kết nối: " + e.getMessage());
        }
    }

    public int checkCardStatus() {
        CommandAPDU cmd = new CommandAPDU(0x00, INS_GET_STATUS, 0x00, 0x00, 1);
        try {
            ResponseAPDU resp = channel.transmit(cmd);
            if (resp.getSW() == SW_SUCCESS) {
                byte[] data = resp.getData();
                if (data.length > 0) {
                    return data[0]; // 0x00 hoặc 0x01
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean setupCard(String initialPin) {
        byte[] pinBytes = initialPin.getBytes();
        byte[] data = new byte[1 + pinBytes.length];

        data[0] = (byte) pinBytes.length; // Byte đầu là độ dài PIN
        System.arraycopy(pinBytes, 0, data, 1, pinBytes.length);

        CommandAPDU cmd = new CommandAPDU(0x00, INS_SETUP_CARD, 0x00, 0x00, data);
        return sendAPDU(cmd);
    }

    public boolean importHostPublicKey(PublicKey publicKey) {
        try {
            RSAPublicKey rsaKey = (RSAPublicKey) publicKey;
            byte[] modulus = adjustKeyComponent(rsaKey.getModulus().toByteArray(), 128);
            byte[] exponent = stripLeadingZero(rsaKey.getPublicExponent().toByteArray());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // 1. Write Exponent Length (2 bytes) & Data
            baos.write(shortToBytes((short) exponent.length));
            baos.write(exponent);

            // 2. Write Modulus Length (2 bytes) & Data
            baos.write(shortToBytes((short) modulus.length));
            baos.write(modulus);

            byte[] data = baos.toByteArray();
            CommandAPDU cmd = new CommandAPDU(0x00, INS_IMPORT_HOST_KEY, 0x00, 0x00, data);
            return sendAPDU(cmd);

        } catch (Exception e) {
            return false;
        }
    }

    // ==================== PIN & SECURITY ====================
    public int verifyPin(String pin) {
        try {
            byte[] pinBytes = pin.getBytes(StandardCharsets.US_ASCII);


            if (pinBytes.length > MAX_PIN_SIZE) {
                pinBytes = Arrays.copyOf(pinBytes, MAX_PIN_SIZE);
            }

            CommandAPDU cmd = new CommandAPDU(0x00, INS_VERIFY_PIN, 0x00, 0x00, pinBytes);
            ResponseAPDU resp = channel.transmit(cmd);

            int sw = resp.getSW();


            if (sw == SW_SUCCESS) {
            } else if (sw == SW_CARD_BLOCKED) {
                System.err.println("ALERT: Thẻ đã bị khóa chức năng (0x6983)!");
            } else if ((sw & 0xFFF0) == 0x63C0) {
                int tries = sw & 0x0F;
                System.err.println("Sai PIN. Số lần còn lại: " + tries);
            } else {
                System.err.println("Lỗi Verify: " + Integer.toHexString(sw));
            }

            return sw;

        } catch (Exception e) {
            e.printStackTrace();
            return SW_UNKNOWN;
        }
    }

    public boolean changePin(String newPin) {
        byte[] pinBytes = newPin.getBytes();
        CommandAPDU cmd = new CommandAPDU(0x00, INS_CHANGE_PIN, 0x00, 0x00, pinBytes);
        return sendAPDU(cmd);
    }

    public boolean unblockCard(String puk) {
        byte[] pukBytes = puk.getBytes();
        CommandAPDU cmd = new CommandAPDU(0x00, INS_UNBLOCK_PIN, 0x00, 0x00, pukBytes);
        return sendAPDU(cmd);
    }

    public boolean setUserInfo(String name, String license, String type) {
        try {
            String raw = name + "|" + license + "|" + type;
            byte[] data = raw.getBytes("UTF-8");
            CommandAPDU cmd = new CommandAPDU(0x00, INS_SET_USER_INFO, 0x00, 0x00, data);
            return sendAPDU(cmd);
        } catch (Exception e) { return false; }
    }

    public String getUserInfo() {
        CommandAPDU cmd = new CommandAPDU(0x00, INS_GET_USER_INFO, 0x00, 0x00, 256);
        try {
            ResponseAPDU resp = channel.transmit(cmd);
            if (resp.getSW() == SW_SUCCESS) {
                byte[] data = resp.getData();
                if (data.length > 0) {
                    return new String(data, "UTF-8").trim();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<String, String> getUserInfoMap(String info) {
        String[] parts = info.split("\\|");
        if (parts.length == 3) {
            Map<String, String> infoMap = new HashMap<>();
            infoMap.put("name", parts[0]);
            infoMap.put("license", parts[1]);
            infoMap.put("type", parts[2]);
            return infoMap;
        }
        return null;
    }

    public int getBalance() {
        CommandAPDU cmd = new CommandAPDU(0x00, INS_GET_BALANCE, 0x00, 0x00, 4);
        try {
            ResponseAPDU resp = channel.transmit(cmd);
            if (resp.getSW() == SW_SUCCESS) {
                byte[] data = resp.getData();
                return bytesToInt(data, 0);
            }
        } catch (Exception e) {}
        return -1;
    }

    public String topUp(int amount, PrivateKey hostPrivateKey, PublicKey cardPublicKey) {
        return performFinancialOp(INS_TOP_UP, amount, hostPrivateKey, cardPublicKey);
    }

    public String pay(int amount, PrivateKey hostPrivateKey, PublicKey cardPublicKey) {
        return performFinancialOp(INS_PAY, amount, hostPrivateKey, cardPublicKey);
    }

    private String performFinancialOp(byte ins, int amount, PrivateKey hostPrivateKey, PublicKey cardPublicKey) {
        try {
            int timestamp = (int) (System.currentTimeMillis() / 1000);

            byte[] payload = new byte[8];
            intToBytes(timestamp, payload, 0);
            intToBytes(amount, payload, 4);

            Signature signer = Signature.getInstance("SHA1withRSA");
            signer.initSign(hostPrivateKey);
            signer.update(payload);
            byte[] signature = signer.sign();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(payload);
            baos.write(signature);
            byte[] apduData = baos.toByteArray();

            CommandAPDU cmd = new CommandAPDU(0x00, ins, 0x00, 0x00, apduData);
            ResponseAPDU resp = channel.transmit(cmd);

            if (resp.getSW() == SW_SUCCESS) {
                byte[] respData = resp.getData();
                if (respData.length > 4) {
                    byte[] balanceBytes = Arrays.copyOfRange(respData, 0, 4);
                    byte[] cardSigBytes = Arrays.copyOfRange(respData, 4, respData.length);
                    Signature verifier = Signature.getInstance("SHA1withRSA");
                    verifier.initVerify(cardPublicKey);
                    verifier.update(balanceBytes);

                    if (verifier.verify(cardSigBytes)) {
                        return Base64.getEncoder().encodeToString(cardSigBytes);
                    } else {
                        System.err.println("CẢNH BÁO: Chữ ký từ thẻ KHÔNG HỢP LỆ! Giao dịch có thể bị giả mạo.");
                        return null;
                    }
                }
            } else {
                System.err.println("Financial Op Error: " + Integer.toHexString(resp.getSW()));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public String getCardID() {
        CommandAPDU cmd = new CommandAPDU(0x00, INS_GET_CARD_ID, 0x00, 0x00, 16);
        try {
            ResponseAPDU resp = channel.transmit(cmd);
            if (resp.getSW() == SW_SUCCESS) {
                byte[] data = resp.getData();
                return new String(data, java.nio.charset.StandardCharsets.US_ASCII);
            }
        } catch (Exception e) {}
        return null;
    }

    public byte[] authenticateCard(byte[] challenge) {
        try {
            CommandAPDU cmd = new CommandAPDU(0x00, INS_AUTHENTICATE_CARD, 0x00, 0x00, challenge);
            ResponseAPDU resp = channel.transmit(cmd);

            if (resp.getSW() != SW_SUCCESS) {
                System.err.println("Authentication failed. SW=" + Integer.toHexString(resp.getSW()));
                return null;
            }

            byte[] signature = resp.getData();

            if (signature == null || signature.length == 0) {
                System.err.println("Empty signature returned.");
                return null;
            }

            return signature;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean uploadImage(File file) {
        try {
            byte[] data = ImageUtils.resizeAndConvert(file);
            int offset = 0;
            while (offset < data.length) {
                int len = Math.min(CHUNK_SIZE, data.length - offset);
                byte[] chunk = new byte[len];
                System.arraycopy(data, offset, chunk, 0, len);
                int p1 = (offset >> 8) & 0xFF;
                int p2 = offset & 0xFF;
                CommandAPDU cmd = new CommandAPDU(0x00, INS_UPLOAD_IMAGE, p1, p2, chunk);
                if (!sendAPDU(cmd)) return false;
                offset += len;
            }
            return true;
        } catch (Exception e) { return false; }
    }

    public byte[] downloadImage() {
        try {
            byte[] fullData = new byte[10240];
            int offset = 0;
            while (offset < 10240) {
                int p1 = (offset >> 8) & 0xFF;
                int p2 = offset & 0xFF;
                CommandAPDU cmd = new CommandAPDU(0x00, INS_DOWNLOAD_IMAGE, p1, p2, CHUNK_SIZE);
                ResponseAPDU resp = channel.transmit(cmd);
                if (resp.getSW() != SW_SUCCESS) break;
                byte[] chunk = resp.getData();
                if (chunk.length == 0) break;
                System.arraycopy(chunk, 0, fullData, offset, chunk.length);
                offset += chunk.length;
                if (chunk.length < CHUNK_SIZE) break;
            }
            return Arrays.copyOf(fullData, offset);
        } catch (Exception e) { return null; }
    }

    public PublicKey getCardPublicKey() {
        try {
            CommandAPDU cmd = new CommandAPDU(0x00, INS_GET_CARD_PUB_KEY, 0x00, 0x00, 256);
            ResponseAPDU resp = channel.transmit(cmd);

            if (resp.getSW() != 0x9000) {
                System.err.println("Lỗi lấy Public Key: " + Integer.toHexString(resp.getSW()));
                return null;
            }

            byte[] data = resp.getData();
            int offset = 0;

            // 1. Đọc độ dài Modulus (2 bytes đầu)
            int modLen = getShort(data, offset);
            offset += 2;

            // 2. Đọc dữ liệu Modulus
            byte[] modulusBytes = new byte[modLen];
            System.arraycopy(data, offset, modulusBytes, 0, modLen);
            offset += modLen;

            // 3. Đọc độ dài Exponent (2 bytes tiếp theo)
            int expLen = getShort(data, offset);
            offset += 2;

            // 4. Đọc dữ liệu Exponent
            byte[] exponentBytes = new byte[expLen];
            System.arraycopy(data, offset, exponentBytes, 0, expLen);

            // 5. Tạo đối tượng RSA PublicKey của Java
            // Lưu ý: Dùng signum=1 để đảm bảo số dương
            BigInteger modulus = new BigInteger(1, modulusBytes);
            BigInteger exponent = new BigInteger(1, exponentBytes);

            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            KeyFactory kf = KeyFactory.getInstance("RSA");

            return kf.generatePublic(spec);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private int getShort(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }
    private boolean sendAPDU(CommandAPDU command) {
        try {
            return channel.transmit(command).getSW() == SW_SUCCESS;
        } catch (Exception e) { return false; }
    }

    private void intToBytes(int val, byte[] buf, int offset) {
        buf[offset] = (byte) (val >> 24);
        buf[offset + 1] = (byte) (val >> 16);
        buf[offset + 2] = (byte) (val >> 8);
        buf[offset + 3] = (byte) (val);
    }

    private int bytesToInt(byte[] buf, int offset) {
        return ((buf[offset] & 0xFF) << 24) |
                ((buf[offset+1] & 0xFF) << 16) |
                ((buf[offset+2] & 0xFF) << 8) |
                (buf[offset+3] & 0xFF);
    }

    private byte[] shortToBytes(short s) {
        return new byte[] { (byte)((s >> 8) & 0xFF), (byte)(s & 0xFF) };
    }

    private byte[] stripLeadingZero(byte[] data) {
        if (data.length > 0 && data[0] == 0) {
            return Arrays.copyOfRange(data, 1, data.length);
        }
        return data;
    }
    private byte[] adjustKeyComponent(byte[] data, int requiredLength) {
        if (data.length == requiredLength) {
            return data;
        }

        byte[] result = new byte[requiredLength];

        if (data.length > requiredLength) {
            System.arraycopy(data, data.length - requiredLength, result, 0, requiredLength);
        } else {
            System.arraycopy(data, 0, result, requiredLength - data.length, data.length);
        }
        return result;
    }
}

