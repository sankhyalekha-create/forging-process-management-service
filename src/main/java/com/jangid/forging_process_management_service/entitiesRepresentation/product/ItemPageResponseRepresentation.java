package com.jangid.forging_process_management_service.entitiesRepresentation.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

@ApiModel(description = "Paginated response representation for items")
public class ItemPageResponseRepresentation {
    
    @ApiModelProperty(value = "List of item representations in the current page")
    @JsonProperty("content")
    private List<ItemRepresentation> content;
    
    @ApiModelProperty(value = "Total number of pages available", example = "5")
    @JsonProperty("totalPages")
    private int totalPages;
    
    @ApiModelProperty(value = "Total number of elements across all pages", example = "50")
    @JsonProperty("totalElements")
    private long totalElements;
    
    @ApiModelProperty(value = "Current page number (0-based)", example = "0")
    @JsonProperty("currentPage")
    private int currentPage;
    
    @ApiModelProperty(value = "Number of elements per page", example = "10")
    @JsonProperty("pageSize")
    private int pageSize;
    
    public ItemPageResponseRepresentation() {}
    
    public ItemPageResponseRepresentation(List<ItemRepresentation> content, 
                                         int totalPages, 
                                         long totalElements, 
                                         int currentPage, 
                                         int pageSize) {
        this.content = content;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }
    
    public List<ItemRepresentation> getContent() { 
        return content; 
    }
    
    public void setContent(List<ItemRepresentation> content) { 
        this.content = content; 
    }
    
    public int getTotalPages() { 
        return totalPages; 
    }
    
    public void setTotalPages(int totalPages) { 
        this.totalPages = totalPages; 
    }
    
    public long getTotalElements() { 
        return totalElements; 
    }
    
    public void setTotalElements(long totalElements) { 
        this.totalElements = totalElements; 
    }
    
    public int getCurrentPage() { 
        return currentPage; 
    }
    
    public void setCurrentPage(int currentPage) { 
        this.currentPage = currentPage; 
    }
    
    public int getPageSize() { 
        return pageSize; 
    }
    
    public void setPageSize(int pageSize) { 
        this.pageSize = pageSize; 
    }
}
