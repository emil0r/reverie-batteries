-- name: sql-get-breadcrumbs

WITH RECURSIVE crumb(id, serial, parent, created, updated, template, name, title, version, slug, route, type, app, "order", depth) AS (
     SELECT DISTINCT
        p.id, serial, parent, p.created, updated, template, name, title, version, slug, route, type, app, "order", 0
     FROM
        reverie_page p
     WHERE
        "version" = :version
        AND serial = :serial

    UNION

    SELECT
        s.id, s.serial, s.parent, s.created, s.updated, s.template, s.name, s.title, s.version, s.slug, s.route, s.type, s.app, s."order", p.depth + 1
    FROM
        reverie_page s
    INNER JOIN
        crumb p ON  s.serial = p.parent
    WHERE
         s."version" = :version
    )
SELECT
    *
FROM
    crumb
ORDER BY
    crumb.depth DESC;
