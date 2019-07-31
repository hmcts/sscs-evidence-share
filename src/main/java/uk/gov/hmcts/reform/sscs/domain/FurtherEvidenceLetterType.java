package uk.gov.hmcts.reform.sscs.domain;

import lombok.Getter;

@Getter
public enum FurtherEvidenceLetterType {

    APPELLANT_LETTER("appellantLetter"),
    REPRESENTATIVE_LETTER("representativeLetter"),
    DWP_LETTER("dwpLetter");

    private String value;

    FurtherEvidenceLetterType(String value) {
        this.value = value;
    }
}
