package org.example.database.DTO;

import java.io.File;
import java.time.LocalDateTime;

public class CardHolderDTO {
    private Integer holderId;
    private String fullName;
    private String phoneNumber;
    private String identityCard;
    private File avatarImage;
    private LocalDateTime createdDate;

    public CardHolderDTO(String fullName, String phoneNumber, String identityCard) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.identityCard = identityCard;
    }
    public CardHolderDTO() {
    }
    public Integer getHolderId() {
        return holderId;
    }

    public void setHolderId(Integer holderId) {
        this.holderId = holderId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getIdentityCard() {
        return identityCard;
    }

    public void setIdentityCard(String identityCard) {
        this.identityCard = identityCard;
    }

    public File getAvatarImage() {
        return avatarImage;
    }

    public void setAvatarImage(File avatarImage) {
        this.avatarImage = avatarImage;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
