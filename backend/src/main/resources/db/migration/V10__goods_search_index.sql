ALTER TABLE goods
    ADD FULLTEXT INDEX idx_goods_search (title, brand, category, story, description);
