package org.papercloud.de.pdfdatabase.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.papercloud.de.core.domain.Document;
import org.papercloud.de.core.domain.Page;
import org.papercloud.de.core.domain.User;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for converting between domain objects and JPA entities.
 */
@Mapper(componentModel = "spring")
public interface DocumentPersistenceMapper {

    // Document mappings
    @Mapping(source = "pages", target = "pages")
    @Mapping(source = "owner", target = "owner")
    Document toDomain(DocumentPdfEntity entity);

    @Mapping(source = "pages", target = "pages")
    @Mapping(source = "owner", target = "owner")
    DocumentPdfEntity toEntity(Document domain);

    List<Document> toDomainList(List<DocumentPdfEntity> entities);

    // Page mappings
    @Mapping(target = "document", ignore = true)
    Page toPageDomain(PagesPdfEntity entity);

    @Mapping(target = "document", ignore = true)
    PagesPdfEntity toPageEntity(Page domain);

    List<Page> toPageDomainList(List<PagesPdfEntity> entities);
    List<PagesPdfEntity> toPageEntityList(List<Page> domains);

    // User mappings
    @Mapping(source = "roles", target = "roles", qualifiedByName = "rolesToStrings")
    User toUserDomain(UserEntity entity);

    @Mapping(target = "roles", ignore = true)  // Roles need special handling
    UserEntity toUserEntity(User domain);

    @Named("rolesToStrings")
    default Set<String> rolesToStrings(Set<org.papercloud.de.pdfdatabase.entity.RoleEntity> roles) {
        if (roles == null) {
            return Set.of();
        }
        return roles.stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());
    }
}
