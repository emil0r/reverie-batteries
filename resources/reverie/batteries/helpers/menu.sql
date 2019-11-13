-- name: sql-get-menu-pages-visibility-visible

WITH RECURSIVE menu(id, serial, parent, created, updated, template, name, title, version, slug, route, type, app, "order", depth) AS (
     SELECT DISTINCT
           p.id, serial, parent, p.created, updated, template, name, title, version, slug, route, type, app, "order", 0
     FROM
           reverie_page p
     LEFT JOIN
          reverie_page_properties pp ON p.serial = pp.page_serial
     WHERE "version" = :version
           AND serial = :root

     UNION

     SELECT
          s.id, s.serial, s.parent, s.created, s.updated, s.template, s.name, s.title, s.version, s.slug, s.route, s.type, s.app, s."order", p.depth + 1
     FROM
          reverie_page s
     INNER JOIN
          menu p ON  s.parent = p.serial
     LEFT JOIN
          reverie_page_properties pp ON s.serial = pp.page_serial
     WHERE
          s."version" = :version
          AND (p.depth + 1) <= :level
          AND (pp.key = 'menu_hide?' AND pp.value = 'false'
               OR pp.key IS NULL)
)
SELECT 
     * 
FROM 
     menu
ORDER BY
     menu.parent DESC, menu.order ASC;

-- name: sql-get-menu-pages-visibility-hidden

WITH RECURSIVE menu(id, serial, parent, created, updated, template, name, title, version, slug, route, type, app, "order", depth) AS (
     SELECT DISTINCT
           p.id, serial, parent, p.created, updated, template, name, title, version, slug, route, type, app, "order", 0
     FROM
           reverie_page p
     LEFT JOIN
          reverie_page_properties pp ON p.serial = pp.page_serial
     WHERE "version" = :version
           AND serial = :root

     UNION

     SELECT
          s.id, s.serial, s.parent, s.created, s.updated, s.template, s.name, s.title, s.version, s.slug, s.route, s.type, s.app, s."order", p.depth + 1
     FROM
          reverie_page s
     INNER JOIN
          menu p ON  s.parent = p.serial
     LEFT JOIN
          reverie_page_properties pp ON s.serial = pp.page_serial
     WHERE
          s."version" = :version
          AND (p.depth + 1) <= :level
          AND pp.key = 'menu_hide?' AND pp.value = 'true'
)
SELECT 
     * 
FROM 
     menu
ORDER BY
     menu.parent DESC, menu.order ASC;


-- name: sql-get-menu-pages-visibility-either

WITH RECURSIVE menu(id, serial, parent, created, updated, template, name, title, version, slug, route, type, app, "order", depth) AS (
     SELECT
           p.id, serial, parent, p.created, updated, template, name, title, version, slug, route, type, app, "order", 0
     FROM
           reverie_page p
     WHERE "version" = :version
           AND serial = :root
     UNION

     SELECT
          s.id, s.serial, s.parent, s.created, s.updated, s.template, s.name, s.title, s.version, s.slug, s.route, s.type, s.app, s."order", p.depth + 1
     FROM
          reverie_page s
     INNER JOIN
          menu p ON  s.parent = p.serial
     WHERE
          s."version" = :version
          AND (p.depth + 1) <= :level
)
SELECT 
     * 
FROM 
     menu
ORDER BY
     menu.parent DESC, menu.order ASC;
