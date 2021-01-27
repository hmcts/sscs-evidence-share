package uk.gov.hmcts.reform.sscs.domain;

import lombok.Getter;

@Getter
public enum FurtherEvidenceLetterType {

    APPELLANT_LETTER("appellantLetter"),
    REPRESENTATIVE_LETTER("representativeLetter"),
    DWP_LETTER("dwpLetter"),
    JOINT_PARTY_LETTER("jointPartyLetter");

    private final String value;

    FurtherEvidenceLetterType(String value) {
        this.value = value;
    }
}
