package com.jangid.forging_process_management_service.entitiesRepresentation.order;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Tenant Order Settings Representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantOrderSettingsRepresentation {

    @JsonProperty("id")
    @ApiModelProperty(value = "Settings ID", example = "1")
    private Long id;

    @JsonProperty("tenantId")
    @ApiModelProperty(value = "Tenant ID", example = "456")
    private Long tenantId;

    // Highlighting Settings
    @JsonProperty("warningDays")
    @ApiModelProperty(value = "Days before deadline to show warning", example = "3")
    @Min(value = 1, message = "Warning days must be at least 1")
    @Max(value = 14, message = "Warning days cannot exceed 14")
    private Integer warningDays;

    @JsonProperty("enableHighlighting")
    @ApiModelProperty(value = "Enable order highlighting", example = "true")
    private Boolean enableHighlighting;

    @JsonProperty("overdueColor")
    @ApiModelProperty(value = "CSS color for overdue orders", example = "#ffebee")
    private String overdueColor;

    @JsonProperty("warningColor")
    @ApiModelProperty(value = "CSS color for warning orders", example = "#fff8e1")
    private String warningColor;

    @JsonProperty("completedColor")
    @ApiModelProperty(value = "CSS color for completed orders", example = "#e8f5e9")
    private String completedColor;

    // Display Settings
    @JsonProperty("autoRefreshInterval")
    @ApiModelProperty(value = "Auto refresh interval in seconds", example = "30")
    @Min(value = 10, message = "Auto refresh interval must be at least 10 seconds")
    @Max(value = 300, message = "Auto refresh interval cannot exceed 300 seconds")
    private Integer autoRefreshInterval;

    @JsonProperty("enableNotifications")
    @ApiModelProperty(value = "Enable browser notifications", example = "true")
    private Boolean enableNotifications;

    @JsonProperty("showCompletedOrders")
    @ApiModelProperty(value = "Show completed orders in main list", example = "true")
    private Boolean showCompletedOrders;

    @JsonProperty("defaultPriority")
    @ApiModelProperty(value = "Default priority for new orders (1=highest, 5=lowest)", example = "3")
    @Min(value = 1, message = "Default priority must be at least 1")
    @Max(value = 5, message = "Default priority cannot exceed 5")
    private Integer defaultPriority;

    @JsonProperty("createdAt")
    @ApiModelProperty(value = "Creation timestamp", example = "2024-10-12T10:30:00")
    private String createdAt;

    @JsonProperty("updatedAt")
    @ApiModelProperty(value = "Last update timestamp", example = "2024-10-12T11:45:00")
    private String updatedAt;
}

