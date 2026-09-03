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
            select distinct p 
            from Post p
            inner join p.tags t
            where t.id in (
                select t2.id from Post p2 join p2.tags t2 where p2.id = :postId
            )
            and p.id != :postId
            and p.status = :status
            order by p.publish desc
                    """)
    List<Post> findSimilaryPosts(
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

    @Query("""
            select p from Post p
            where p.status = :status
            and (:cursor is null or p.id < :cursor)
            order by p.id desc
            """)
    List<Post> findByStatusKeyset(
            @Param("status") Status status,
            @Param("cursor") Long cursor,
            Pageable pageable);
}
