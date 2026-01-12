package com.devhub.project.dto;

import com.devhub.common.entity.BaseEntity;
import java.util.List;

public class ProjectResponse extends BaseEntity {
    public String title;
    public String description;
    public String imageUrl;
    public String ownerId;
    public List<String> memberIds;
}

