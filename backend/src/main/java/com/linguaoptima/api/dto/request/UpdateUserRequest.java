package com.linguaoptima.api.dto.request;

import com.linguaoptima.api.domain.enums.CefrLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    private String fullName;
    private String displayAlias;
    private CefrLevel cefrLevel;
}
