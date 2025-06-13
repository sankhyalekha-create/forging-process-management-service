package com.jangid.forging_process_management_service.entitiesRepresentation.product;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Item representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItemRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the rawMaterial", example = "123")
  private Long id;

  @JsonProperty("itemName")
  @ApiModelProperty(value = "itemName")
  private String itemName;

  @JsonProperty("itemCode")
  @ApiModelProperty(value = "itemCode")
  private String itemCode;

  @JsonProperty("itemWeight")
  @ApiModelProperty(value = "Weight of the item")
  private String itemWeight;

  @JsonProperty("itemForgedWeight")
  @ApiModelProperty(value = "Forged weight of the item")
  private String itemForgedWeight;

  @JsonProperty("itemSlugWeight")
  @ApiModelProperty(value = "Slug weight of the item")
  private String itemSlugWeight;

  @JsonProperty("itemFinishedWeight")
  @ApiModelProperty(value = "Finished weight of the item")
  private String itemFinishedWeight;

  @JsonProperty("itemCount")
  @ApiModelProperty(value = "Count of the item when measured in PIECES")
  private String itemCount;

  @JsonProperty("itemProducts")
  @ApiModelProperty(value = "List of products of item")
  private List<ItemProductRepresentation> itemProducts;

  @JsonProperty("workflowTemplateId")
  @ApiModelProperty(value = "ID of the workflow template to use for this item (required)")
  private Long workflowTemplateId;

  @JsonProperty("workflowTemplateName")
  @ApiModelProperty(value = "Name of the workflow template associated with this item")
  private String workflowTemplateName;

  @JsonProperty("workflowStatus")
  @ApiModelProperty(value = "Current status of the item workflow")
  private String workflowStatus;

  @JsonProperty("currentStepName")
  @ApiModelProperty(value = "Name of the current workflow step")
  private String currentStepName;

  @JsonProperty("workflowSteps")
  @ApiModelProperty(value = "List of workflow steps for this item")
  private List<String> workflowSteps;

  @JsonProperty("tenantId")
  @ApiModelProperty(value = "tenantId")
  private Long tenantId;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp at which the item entity was created")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp at which the item entity was updated")
  private String updatedAt;

  @JsonProperty("deletedAt")
  @ApiModelProperty(value = "Timestamp at which the item entity was deleted")
  private String deletedAt;
}
