package com.example.dao;

import com.example.domain.Author;

import java.util.List;

/**
 * Created by huanglei on 17/3/28.
 */
public interface AuthorDao {
    int add(Author author);
    int update(Author author);
    int delete(Long id);
    Author findAuthor(Long id);
    List<Author> findAuthorList();
}
