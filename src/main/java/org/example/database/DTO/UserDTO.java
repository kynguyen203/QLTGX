package org.example.database.DTO;

import java.io.File;
import java.math.BigDecimal;

public class UserDTO {
    private String name;
    private String phone;
    private String identityCard;
    private String licensePlate;
    private String vehicleType;
    private String cardUID;
    private BigDecimal balance;
    private String publicKey;
    private File imageFile;

    public UserDTO(String name, String phone, String identityCard, String licensePlate, String vehicleType, String cardUID, String publicKey, File imageFile) {
        this.name = name;
        this.phone = phone;
        this.identityCard = identityCard;
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
        this.cardUID = cardUID;
        this.publicKey = publicKey;
        this.imageFile = imageFile;
    }
    public UserDTO(String name, String phone, String identityCard, String licensePlate, String vehicleType, String cardUID, File imageFile) {
        this.name = name;
        this.phone = phone;
        this.identityCard = identityCard;
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
        this.cardUID = cardUID;
        this.imageFile = imageFile;
    }
    public UserDTO(String name, String phone, String identityCard, String licensePlate, String vehicleType,String cardUID, BigDecimal balance) {
        this.name = name;
        this.phone = phone;
        this.identityCard = identityCard;
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
        this.balance = balance;
        this.cardUID = cardUID;
    }

    // Getters
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getIdentityCard() { return identityCard; }
    public String getLicensePlate() { return licensePlate; }
    public String getVehicleType() { return vehicleType; }
    public String getCardUID() { return cardUID; }
    public BigDecimal getBalance() { return balance; }
    public String getPublicKey() { return publicKey; }
    public File getImageFile() { return imageFile; }


}
