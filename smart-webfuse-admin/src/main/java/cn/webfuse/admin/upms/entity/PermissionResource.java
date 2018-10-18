package cn.webfuse.admin.upms.entity;

import cn.webfuse.admin.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class PermissionResource extends BaseEntity<Long> {

    private static final long serialVersionUID = 1L;

    private Long permissionId;

    private Long resourceId;


}
