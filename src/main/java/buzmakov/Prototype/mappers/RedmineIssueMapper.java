package buzmakov.Prototype.mappers;

import buzmakov.Prototype.model.AuthorRole;
import buzmakov.Prototype.model.SupportCase;
import com.taskadapter.redmineapi.bean.Issue;
import lombok.val;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static java.util.Optional.ofNullable;

@Mapper(componentModel = "spring")
public interface RedmineIssueMapper {

    @Mapping(target = "role", expression = "java(determineRole(issue))")
    @Mapping(source = "description", target = "text")
    SupportCase toModel(final Issue issue);

    default AuthorRole determineRole(final Issue issue) {
        val trackerName = ofNullable(issue.getTracker())
            .map(tracker -> tracker.getName())
            .orElse("");

        if (trackerName.equalsIgnoreCase("Поддержка")) {
            return AuthorRole.SUPPORT;
        }

        return AuthorRole.CLIENT;
    }
}