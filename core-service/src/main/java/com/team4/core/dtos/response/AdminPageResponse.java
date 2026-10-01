package com.team4.core.dtos.response;

import java.util.List;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminPageResponse<T> {
    List<T> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
    boolean first;
    boolean last;

    public static <S, T> AdminPageResponse<T> from(Page<S> source, Function<S, T> mapper) {
        return AdminPageResponse.<T>builder()
                .content(source.getContent().stream().map(mapper).toList())
                .page(source.getNumber())
                .size(source.getSize())
                .totalElements(source.getTotalElements())
                .totalPages(source.getTotalPages())
                .first(source.isFirst())
                .last(source.isLast())
                .build();
    }
}
