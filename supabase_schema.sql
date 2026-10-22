-- =============================================================================
-- СХЕМА БАЗЫ ДАННЫХ ДЛЯ ПРИЛОЖЕНИЯ «СБРОС» (SUPABASE / POSTGRESQL)
-- Разработано для курсовой работы: реляционная модель, RLS, триггеры, soft-delete
-- =============================================================================

-- Включаем расширение для генерации UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- -----------------------------------------------------------------------------
-- 1. ТАБЛИЦА ПРОФИЛЕЙ ПОЛЬЗОВАТЕЛЕЙ (profiles)
-- Привязана 1:1 к встроенной auth.users Supabase
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT,
    display_name TEXT DEFAULT 'Данила',
    avatar_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------------
-- 2. ТАБЛИЦА КОНТЕКСТОВ / СФЕР ЖИЗНИ (contexts)
-- Хранит контексты пользователя (Учеба, Код, Покупки, Быт и т.д.)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.contexts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    icon TEXT NOT NULL DEFAULT 'label',
    color_hex TEXT DEFAULT '#554F87',
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

-- -----------------------------------------------------------------------------
-- 3. ТАБЛИЦА БОЛЬШИХ ДЕЛ / ПРОЕКТОВ (projects)
-- Хранит многоэтапные проекты (например: "Курсовая работа")
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    is_archived BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

-- -----------------------------------------------------------------------------
-- 4. ТАБЛИЦА ЗАДАЧ (tasks)
-- Центральная таблица GTD-системы
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.tasks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    project_id UUID REFERENCES public.projects(id) ON DELETE SET NULL,
    order_in_project INT NOT NULL DEFAULT 0,
    title TEXT NOT NULL,
    original_quote TEXT DEFAULT '',
    difficulty TEXT NOT NULL DEFAULT 'EASY', -- EASY, MEDIUM, HARD
    shelf TEXT NOT NULL DEFAULT 'SIMPLE_STEPS', -- SIMPLE_STEPS, BIG_THINGS, WAITING, SOMEDAY, REFERENCE
    is_completed BOOLEAN NOT NULL DEFAULT false,
    current_simplify_index INT NOT NULL DEFAULT 0,
    ai_hint TEXT DEFAULT '',
    category_badge TEXT DEFAULT '',
    estimated_minutes INT NOT NULL DEFAULT 5,
    skipped_count INT NOT NULL DEFAULT 0,
    last_shown_at BIGINT NOT NULL DEFAULT 0,
    ai_draft_type TEXT DEFAULT '',
    ai_draft_content TEXT DEFAULT '',
    ai_advice TEXT DEFAULT '',
    scheduled_date_millis BIGINT,
    scheduled_time_text TEXT,
    context_tag TEXT NOT NULL DEFAULT 'home',
    context_category TEXT NOT NULL DEFAULT 'Дом и быт',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

-- -----------------------------------------------------------------------------
-- 5. ТАБЛИЦА ПОДШАГОВ ДЕКОМПОЗИЦИИ (task_substeps)
-- Связь 1:M с tasks, каскадное удаление при удалении задачи
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.task_substeps (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id UUID NOT NULL REFERENCES public.tasks(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    text TEXT NOT NULL,
    is_done BOOLEAN NOT NULL DEFAULT false,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

-- =============================================================================
-- ИНДЕКСЫ ДЛЯ УСКОРЕНИЯ ВЫБОРКИ И СИНХРОНИЗАЦИИ
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_tasks_user_updated ON public.tasks(user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_tasks_project ON public.tasks(project_id);
CREATE INDEX IF NOT EXISTS idx_tasks_is_completed ON public.tasks(user_id, is_completed, is_deleted);
CREATE INDEX IF NOT EXISTS idx_substeps_task ON public.task_substeps(task_id);
CREATE INDEX IF NOT EXISTS idx_projects_user ON public.projects(user_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_contexts_user ON public.contexts(user_id, is_deleted);

-- =============================================================================
-- ROW LEVEL SECURITY (RLS) — ИЗОЛЯЦИЯ ДАННЫХ ПОЛЬЗОВАТЕЛЕЙ
-- Ни один пользователь не имеет доступа к данным другого пользователя
-- =============================================================================
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.contexts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.task_substeps ENABLE ROW LEVEL SECURITY;

-- Политики для profiles
CREATE POLICY "Users can manage own profile"
    ON public.profiles FOR ALL
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

-- Политики для contexts
CREATE POLICY "Users can manage own contexts"
    ON public.contexts FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Политики для projects
CREATE POLICY "Users can manage own projects"
    ON public.projects FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Политики для tasks
CREATE POLICY "Users can manage own tasks"
    ON public.tasks FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Политики для task_substeps
CREATE POLICY "Users can manage own task substeps"
    ON public.task_substeps FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- =============================================================================
-- ТРИГГЕРЫ АВТОМАТИЗАЦИИ
-- =============================================================================

-- Функция автоматического обновления поля updated_at при любом изменении строки
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_tasks_updated_at
    BEFORE UPDATE ON public.tasks
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

CREATE OR REPLACE TRIGGER trg_substeps_updated_at
    BEFORE UPDATE ON public.task_substeps
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

CREATE OR REPLACE TRIGGER trg_projects_updated_at
    BEFORE UPDATE ON public.projects
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

CREATE OR REPLACE TRIGGER trg_contexts_updated_at
    BEFORE UPDATE ON public.contexts
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- Функция инициализации профиля и стартовых категорий при регистрации нового пользователя
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    -- Создаем профиль
    INSERT INTO public.profiles (id, email, display_name)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'display_name', 'Данила')
    );

    -- Создаем 4 стандартных контекста GTD для нового пользователя
    INSERT INTO public.contexts (user_id, name, icon, sort_order)
    VALUES
        (NEW.id, 'Учеба и вуз', 'school', 1),
        (NEW.id, 'Разработка и код', 'code', 2),
        (NEW.id, 'Покупки и заказы', 'shopping_bag', 3),
        (NEW.id, 'Дом и быт', 'home', 4);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Привязываем триггер к таблице auth.users
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
