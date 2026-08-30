package com.spring.blog.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.spring.blog.model.Post;
import com.spring.blog.model.Status;
import com.spring.blog.model.TagModel;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByStatusOrderByPublishDesc(Status status);

    Optional<Post> findBySlugAndStatus(String slug, Status status);

    Page<Post> findByStatusOrderByPublishDesc(Status status, Pageable pageable);

    List<Post> findByTagsContaining(TagModel tag);

    @Query("""
                select p from Post p
                join p.tags t
                where t in :tags
                and p.id != :postId
                and p.status = :status
                group by p
                order by count(t) desc, p.publish desc
            """)
    List<Post> findSimilaryPosts(
            @Param("tags") Set<TagModel> tags,
            @Param("postId") Long postId,
            @Param("status") Status status,
            Pageable pageable);

    @Query(value = """
                select * from posts
                where status = 'PUBLISHED'
                and search_vector @@ plainto_tsquery('english', :query)
                order by ts_rank(search_vector, plainto_tsquery('english', :query)) desc
            """, nativeQuery = true)
    List<Post> fullTextSearch(@Param("query") String query);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.tags WHERE p.slug = :slug AND p.status = :status")
    Optional<Post> findBySlugAndStatusWithTags(@Param("slug") String slug, @Param("status") Status status);

}
