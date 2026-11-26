-- LIFTLOG 기본 운동 데이터
-- 기본 운동 34종 등록

MERGE INTO exercise e
    USING (
        -- 가슴 (6종)
        SELECT '벤치프레스' AS exercise_name,
               '가슴' AS body_part FROM dual
        UNION ALL SELECT '인클라인 바벨 프레스', '가슴' FROM dual
        UNION ALL SELECT '인클라인 덤벨 프레스', '가슴' FROM dual
        UNION ALL SELECT '팩덱 플라이', '가슴' FROM dual
        UNION ALL SELECT '딥스', '가슴' FROM dual
        UNION ALL SELECT '체스트 프레스', '가슴' FROM dual

        -- 등 (7종)
        UNION ALL SELECT '풀업', '등' FROM dual
        UNION ALL SELECT '랫풀다운', '등' FROM dual
        UNION ALL SELECT '티바로우', '등' FROM dual
        UNION ALL SELECT '시티드로우', '등' FROM dual
        UNION ALL SELECT '바벨로우', '등' FROM dual
        UNION ALL SELECT '데드리프트', '등' FROM dual
        UNION ALL SELECT '슈퍼하이로우', '등' FROM dual

        -- 하체 (7종)
        UNION ALL SELECT '바벨 스쿼트', '하체' FROM dual
        UNION ALL SELECT '레그프레스', '하체' FROM dual
        UNION ALL SELECT '레그컬', '하체' FROM dual
        UNION ALL SELECT '핵스쿼트', '하체' FROM dual
        UNION ALL SELECT '레그 익스텐션', '하체' FROM dual
        UNION ALL SELECT '힙 어브덕션', '하체' FROM dual
        UNION ALL SELECT '카프 레이즈', '하체' FROM dual

        -- 어깨 (5종)
        UNION ALL SELECT '덤벨 숄더프레스', '어깨' FROM dual
        UNION ALL SELECT '머신 숄더프레스', '어깨' FROM dual
        UNION ALL SELECT '사이드 레터럴 레이즈', '어깨' FROM dual
        UNION ALL SELECT '리버스 팩덱 플라이', '어깨' FROM dual
        UNION ALL SELECT '오버헤드 프레스', '어깨' FROM dual

        -- 팔 (6종)
        UNION ALL SELECT '바벨 컬', '팔' FROM dual
        UNION ALL SELECT '덤벨 컬', '팔' FROM dual
        UNION ALL SELECT '해머 컬', '팔' FROM dual
        UNION ALL SELECT '케이블 푸시다운', '팔' FROM dual
        UNION ALL SELECT '오버헤드 트라이셉스 익스텐션', '팔' FROM dual
        UNION ALL SELECT '프리처 컬', '팔' FROM dual

        -- 복근 (3종)
        UNION ALL SELECT '행잉 레그레이즈', '복근' FROM dual
        UNION ALL SELECT 'AB 슬라이드', '복근' FROM dual
        UNION ALL SELECT '케이블 크런치', '복근' FROM dual
    ) s
    ON (
        e.exercise_name = s.exercise_name
            AND e.body_part = s.body_part
            AND e.registered_member_id IS NULL
        )
    WHEN NOT MATCHED THEN
        INSERT (exercise_name, body_part)
            VALUES (s.exercise_name, s.body_part);

COMMIT;

-- 부위별 기본 운동 개수 확인
SELECT body_part, COUNT(*) AS exercise_count
FROM exercise
WHERE registered_member_id IS NULL
GROUP BY body_part
ORDER BY body_part;

-- 전체 기본 운동 개수 확인
SELECT COUNT(*) AS total_exercises
FROM exercise
WHERE registered_member_id IS NULL;