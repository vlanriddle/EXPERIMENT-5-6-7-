import { useEffect, useState, type FormEvent } from 'react'
import {
  ArrowDownUp, ArrowLeft, ArrowRight, CalendarClock, Check, ChevronDown,
  CircleHelp, Clock3, FileText, ImagePlus, LogOut, Megaphone, MoreHorizontal,
  Pencil, Plus, Send, Trash2, TrendingUp, X,
} from 'lucide-react'
import { apiErrorMessage, authApi, postsApi, setAccessToken, type AuthUser, type PostInput, type PostItem, type PostStatus } from './api'
import './App.css'

function App() {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [checking, setChecking] = useState(true)
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [authError, setAuthError] = useState('')
  const [busyAuth, setBusyAuth] = useState(false)
  const [posts, setPosts] = useState<PostItem[]>([])
  const [queue, setQueue] = useState<PostItem[]>([])
  const [total, setTotal] = useState(0)
  const [pages, setPages] = useState(0)
  const [page, setPage] = useState(0)
  const [filter, setFilter] = useState<'ALL' | PostStatus>('ALL')
  const [ascending, setAscending] = useState(false)
  const [content, setContent] = useState('')
  const [mediaUrl, setMediaUrl] = useState('')
  const [status, setStatus] = useState<PostStatus>('PUBLISHED')
  const [scheduledAt, setScheduledAt] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let active = true
    authApi.refresh().then((session) => {
      if (active) { setAccessToken(session.accessToken); setUser(session) }
    }).catch(() => setAccessToken(null)).finally(() => { if (active) setChecking(false) })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!user) return
    let active = true
    Promise.all([
      postsApi.list({ page, size: 8, ...(filter === 'ALL' ? {} : { status: filter }), sortBy: 'createdAt', sortDir: ascending ? 'asc' : 'desc' }),
      postsApi.list({ page: 0, size: 5, status: 'SCHEDULED', sortBy: 'scheduledAt', sortDir: 'asc' }),
    ]).then(([result, upcoming]) => {
      if (!active) return
      setPosts(result.content)
      setTotal(result.totalElements)
      setPages(result.totalPages)
      setQueue(upcoming.content)
    }).catch((reason: unknown) => { if (active) setError(apiErrorMessage(reason)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [user, page, filter, ascending])

  async function submitAuth(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusyAuth(true)
    setAuthError('')
    try {
      const session = authMode === 'register'
        ? await authApi.register(email, displayName, password)
        : await authApi.login(email, password)
      setAccessToken(session.accessToken)
      setUser(session)
    } catch (reason) { setAuthError(apiErrorMessage(reason)) }
    finally { setBusyAuth(false) }
  }

  async function logout() {
    try { await authApi.logout() } catch { /* Expired sessions are cleared locally. */ }
    setAccessToken(null)
    setUser(null)
    setPosts([])
  }

  function clearComposer() {
    setContent('')
    setMediaUrl('')
    setStatus('PUBLISHED')
    setScheduledAt('')
    setEditingId(null)
  }

  async function savePost(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')
    const input: PostInput = {
      content: content.trim(),
      mediaUrl: mediaUrl.trim() || null,
      status,
      scheduledAt: status === 'SCHEDULED' ? new Date(scheduledAt).toISOString() : null,
    }
    try {
      const wasEditing = Boolean(editingId)
      if (editingId) await postsApi.update(editingId, input)
      else await postsApi.create(input)
      clearComposer()
      setPage(0)
      setNotice(wasEditing ? 'Post updated' : status === 'SCHEDULED' ? 'Post scheduled' : status === 'DRAFT' ? 'Draft saved' : 'Post published')
      await reloadFirstPage()
    } catch (reason) { setError(apiErrorMessage(reason)) }
    finally { setSaving(false) }
  }

  async function reloadFirstPage() {
    const [result, upcoming] = await Promise.all([
      postsApi.list({ page: 0, size: 8, ...(filter === 'ALL' ? {} : { status: filter }), sortBy: 'createdAt', sortDir: ascending ? 'asc' : 'desc' }),
      postsApi.list({ page: 0, size: 5, status: 'SCHEDULED', sortBy: 'scheduledAt', sortDir: 'asc' }),
    ])
    setPosts(result.content)
    setTotal(result.totalElements)
    setPages(result.totalPages)
    setQueue(upcoming.content)
  }

  function editPost(post: PostItem) {
    setEditingId(post.id)
    setContent(post.content)
    setMediaUrl(post.mediaUrl ?? '')
    setStatus(post.status)
    setScheduledAt(post.scheduledAt ? toLocalDateTime(post.scheduledAt) : '')
    document.getElementById('composer')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }

  async function deletePost(post: PostItem) {
    if (!window.confirm('Delete this post? This cannot be undone.')) return
    try {
      await postsApi.remove(post.id)
      setNotice('Post deleted')
      await reloadFirstPage()
    } catch (reason) { setError(apiErrorMessage(reason)) }
  }

  if (checking) return <main className="loading-screen"><span className="loading-mark"><Megaphone size={21} /></span><p>Opening your workspace</p></main>

  if (!user) return (
    <main className="auth-screen">
      <section className="auth-aside">
        <a className="brand" href="#home"><span className="brand-mark"><Megaphone size={19} /></span><span>postroom</span></a>
        <div className="auth-note"><span className="eyebrow">SOCIAL, IN GOOD ORDER</span><h1>Make room for the work worth sharing.</h1><p>A quieter place to draft, plan, and publish your next good thing.</p><div className="auth-stamp"><CalendarClock size={18} /><span>One calendar.<br />Every idea in motion.</span></div></div>
        <div className="auth-foot"><span>POSTROOM STUDIO</span><span>01 / 04</span></div>
      </section>
      <section className="auth-panel"><div className="auth-box">
        <div className="auth-mobile-brand"><span className="brand-mark"><Megaphone size={19} /></span> postroom</div>
        <span className="eyebrow">{authMode === 'login' ? 'WELCOME BACK' : 'START A WORKSPACE'}</span>
        <h2>{authMode === 'login' ? 'Sign in to your desk' : 'Create your account'}</h2>
        <p className="auth-intro">{authMode === 'login' ? 'Pick up where your next post begins.' : 'Your publishing calendar starts here.'}</p>
        <form className="auth-form" onSubmit={submitAuth}>
          {authMode === 'register' && <label>Your name<input autoComplete="name" value={displayName} onChange={(event) => setDisplayName(event.target.value)} minLength={2} maxLength={80} required /></label>}
          <label>Email address<input type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} maxLength={320} required /></label>
          <label>Password<input type="password" autoComplete={authMode === 'login' ? 'current-password' : 'new-password'} value={password} onChange={(event) => setPassword(event.target.value)} minLength={authMode === 'login' ? 1 : 12} maxLength={72} required /></label>
          {authError && <p className="form-error" role="alert">{authError}</p>}
          <button className="button button-primary auth-submit" type="submit" disabled={busyAuth}>{busyAuth ? 'Please wait…' : authMode === 'login' ? 'Sign in' : 'Create account'}<ArrowRight size={16} /></button>
        </form>
        <p className="auth-switch">{authMode === 'login' ? 'New to postroom?' : 'Already have an account?'} <button type="button" onClick={() => { setAuthMode(authMode === 'login' ? 'register' : 'login'); setAuthError('') }}>{authMode === 'login' ? 'Create an account' : 'Sign in'}</button></p>
      </div><span className="auth-privacy"><CircleHelp size={14} /> Refresh sessions use an HTTP-only cookie.</span></section>
    </main>
  )

  const greeting = user.displayName.split(' ')[0]
  return (
    <div className="app-frame">
      <header className="topbar">
        <a className="brand" href="#home"><span className="brand-mark"><Megaphone size={19} /></span><span>postroom</span></a>
        <div className="topbar-divider" />
        <button className="workspace-label"><span className="workspace-dot" />Personal workspace <ChevronDown size={14} /></button>
        <div className="topbar-spacer" />
        <button className="icon-button help-button" title="Help" aria-label="Help"><CircleHelp size={18} /></button>
        <div className="profile-wrap"><span className="profile-avatar">{initials(user.displayName)}</span><span className="profile-name">{user.displayName}</span></div>
        <button className="icon-button" onClick={logout} title="Sign out" aria-label="Sign out"><LogOut size={17} /></button>
      </header>
      <div className="app-layout">
        <aside className="sidebar">
          <div className="sidebar-label">PUBLISHING</div>
          <nav className="side-nav" aria-label="Post status filters">
            <NavItem active={filter === 'ALL'} label="All posts" count={filter === 'ALL' ? total : undefined} icon={<FileText size={17} />} onClick={() => { setFilter('ALL'); setPage(0) }} />
            <NavItem active={filter === 'PUBLISHED'} label="Published" icon={<Send size={17} />} onClick={() => { setFilter('PUBLISHED'); setPage(0) }} />
            <NavItem active={filter === 'SCHEDULED'} label="Scheduled" count={queue.length || undefined} icon={<CalendarClock size={17} />} onClick={() => { setFilter('SCHEDULED'); setPage(0) }} />
            <NavItem active={filter === 'DRAFT'} label="Drafts" icon={<Pencil size={17} />} onClick={() => { setFilter('DRAFT'); setPage(0) }} />
          </nav>
          <div className="sidebar-bottom"><div className="sidebar-divider" /><div className="plan-label">YOUR STUDIO</div>
            <div className="studio-card"><span className="studio-icon"><TrendingUp size={17} /></span><div><strong>Good things grow</strong><span>Keep your cadence going.</span></div></div>
            <div className="sidebar-user"><span className="profile-avatar small">{initials(user.displayName)}</span><span><strong>{user.displayName}</strong><small>Personal account</small></span><MoreHorizontal size={17} /></div>
          </div>
        </aside>
        <main className="main-content">
          <div className="page-heading"><div><div className="eyebrow">YOUR CONTENT DESK <span className="heading-slash">/</span> {greeting.toUpperCase()}</div><h1>Good work, in motion.</h1><p>Shape the next thing you want people to see.</p></div><button className="button button-primary heading-action" onClick={() => { clearComposer(); document.getElementById('composer')?.scrollIntoView({ behavior: 'smooth', block: 'center' }) }}><Plus size={17} /> New post</button></div>
          <section className="stats-row" aria-label="Publishing overview">
            <Stat label="POSTS IN VIEW" value={total} foot="across this filter" />
            <Stat label="UP NEXT" value={queue.length} foot="scheduled to publish" />
            <Stat label="CURRENT VIEW" value={filter === 'ALL' ? 'All' : titleCase(filter)} foot={<><span className="live-dot" /> your content desk</>} highlight />
          </section>
          <div className="content-grid">
            <section className="feed-column">
              <section className={editingId ? 'composer editing' : 'composer'} id="composer">
                <div className="composer-top"><div className="composer-title"><span className="composer-avatar">{initials(user.displayName)}</span><div><strong>{editingId ? 'Edit your post' : 'Start with a thought'}</strong><span>{editingId ? 'Refine it before it goes out.' : 'What would you like to share?'}</span></div></div>{editingId && <button className="icon-button" title="Cancel editing" aria-label="Cancel editing" onClick={clearComposer}><X size={17} /></button>}</div>
                <form onSubmit={savePost}>
                  <textarea value={content} onChange={(event) => setContent(event.target.value)} maxLength={2000} placeholder="Write something worth passing along…" aria-label="Post content" required />
                  <div className="composer-meta"><button className="composer-tool" type="button" onClick={() => document.getElementById('media-url')?.focus()}><ImagePlus size={16} /> Add a link</button><span className={content.length > 1850 ? 'char-count near-limit' : 'char-count'}>{content.length.toLocaleString()} / 2,000</span></div>
                  <label className="sr-only" htmlFor="media-url">Image URL</label><input className="media-input" id="media-url" type="url" placeholder="https://… image URL (optional)" value={mediaUrl} onChange={(event) => setMediaUrl(event.target.value)} maxLength={2048} />
                  <div className="composer-footer"><div className="publish-controls"><label className="sr-only" htmlFor="publish-status">Post status</label><select id="publish-status" value={status} onChange={(event) => setStatus(event.target.value as PostStatus)}><option value="PUBLISHED">Publish now</option><option value="DRAFT">Save as draft</option><option value="SCHEDULED">Schedule for later</option></select>{status === 'SCHEDULED' && <label className="schedule-input"><Clock3 size={15} /><input type="datetime-local" value={scheduledAt} onChange={(event) => setScheduledAt(event.target.value)} min={toLocalDateTime(new Date(Date.now() + 60000).toISOString())} required aria-label="Schedule date and time" /></label>}</div><button className="button button-primary composer-submit" type="submit" disabled={saving || !content.trim() || (status === 'SCHEDULED' && !scheduledAt)}>{saving ? 'Saving…' : editingId ? 'Save changes' : status === 'SCHEDULED' ? 'Schedule post' : status === 'DRAFT' ? 'Save draft' : 'Publish'}{!saving && <ArrowRight size={15} />}</button></div>
                </form>
              </section>
              <div className="feed-heading"><div><span className="eyebrow">YOUR LIBRARY</span><h2>Recent posts</h2></div><button className="sort-button" onClick={() => setAscending(!ascending)} title={`Sort ${ascending ? 'newest first' : 'oldest first'}`}><ArrowDownUp size={15} /><span>{ascending ? 'Oldest' : 'Newest'}</span></button></div>
              <div className="filter-tabs" role="group" aria-label="Filter posts">{(['ALL', 'PUBLISHED', 'SCHEDULED', 'DRAFT'] as const).map((item) => <button key={item} className={filter === item ? 'filter-tab selected' : 'filter-tab'} onClick={() => { setFilter(item); setPage(0) }}>{item === 'ALL' ? 'All' : titleCase(item)}</button>)}</div>
              {notice && <div className="notice" role="status"><Check size={15} />{notice}<button title="Dismiss" aria-label="Dismiss notification" onClick={() => setNotice('')}><X size={15} /></button></div>}
              {error && <div className="inline-error" role="alert">{error}<button title="Dismiss error" aria-label="Dismiss error" onClick={() => setError('')}><X size={15} /></button></div>}
              <div className="post-list" aria-live="polite">{loading ? <div className="empty-state"><span className="loading-mark"><Megaphone size={19} /></span><p>Loading your posts…</p></div> : posts.length === 0 ? <div className="empty-state"><span className="empty-icon"><FileText size={21} /></span><h3>Nothing on this page yet</h3><p>Start with a draft, publish now, or set it up for later.</p><button className="text-action" onClick={() => document.getElementById('composer')?.scrollIntoView({ behavior: 'smooth', block: 'center' })}>Write a post <ArrowRight size={14} /></button></div> : posts.map((post, index) => <PostCard key={post.id} post={post} index={index} onEdit={editPost} onDelete={deletePost} />)}</div>
              {pages > 1 && <div className="pagination"><span>{page * 8 + 1}–{Math.min((page + 1) * 8, total)} of {total}</span><div><button className="icon-button page-arrow" disabled={page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))} title="Previous page" aria-label="Previous page"><ArrowLeft size={16} /></button><span className="page-indicator">{page + 1} <i>/</i> {pages}</span><button className="icon-button page-arrow" disabled={page >= pages - 1} onClick={() => setPage((current) => Math.min(pages - 1, current + 1))} title="Next page" aria-label="Next page"><ArrowRight size={16} /></button></div></div>}
            </section>
            <aside className="right-rail">
              <section className="rail-section"><div className="rail-heading"><div><span className="eyebrow">THE QUEUE</span><h2>Coming up</h2></div><CalendarClock size={18} /></div>{queue.length === 0 ? <div className="queue-empty"><span className="queue-clock"><Clock3 size={18} /></span><p>Your next post can be right on time.</p><button className="text-action" onClick={() => { setStatus('SCHEDULED'); document.getElementById('composer')?.scrollIntoView({ behavior: 'smooth', block: 'center' }) }}>Plan a post <ArrowRight size={14} /></button></div> : <div className="queue-list">{queue.map((post) => <div className="queue-item" key={post.id}><span className="queue-date">{formatQueueDate(post.scheduledAt)}</span><p>{post.content}</p><span className="queue-time"><Clock3 size={12} />{post.scheduledAt ? new Date(post.scheduledAt).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' }) : 'Time pending'}</span></div>)}</div>}</section>
              <section className="rail-note"><span className="note-mark"><TrendingUp size={16} /></span><div><span className="eyebrow">A SMALL REMINDER</span><h3>Consistency beats the perfect post.</h3><p>Keep showing up in your own voice.</p></div><span className="note-index">NOTE 01</span></section>
              <div className="rail-footer"><span>POSTROOM STUDIO</span><span>CONTENT DESK · 2026</span></div>
            </aside>
          </div>
          <footer className="mobile-footer"><span>POSTROOM STUDIO</span><span>CONTENT DESK · 2026</span></footer>
        </main>
      </div>
    </div>
  )
}

function NavItem({ active, label, count, icon, onClick }: { active: boolean; label: string; count?: number; icon: React.ReactNode; onClick: () => void }) {
  return <button className={active ? 'nav-item active' : 'nav-item'} onClick={onClick}>{icon}<span>{label}</span><span className="nav-count">{count ?? ''}</span></button>
}

function Stat({ label, value, foot, highlight = false }: { label: string; value: number | string; foot: React.ReactNode; highlight?: boolean }) {
  return <div className={highlight ? 'stat-cell stat-highlight' : 'stat-cell'}><span className="stat-label">{label}</span><strong>{typeof value === 'number' ? String(value).padStart(2, '0') : value}</strong><span className="stat-foot">{foot}</span></div>
}

function PostCard({ post, index, onEdit, onDelete }: { post: PostItem; index: number; onEdit: (post: PostItem) => void; onDelete: (post: PostItem) => void }) {
  return <article className="post-card" style={{ animationDelay: `${Math.min(index * 55, 220)}ms` }}>
    <div className="post-card-top"><div className="post-author"><span className="post-avatar">{initials(post.authorName)}</span><div><strong>{post.authorName}</strong><span>{post.status === 'SCHEDULED' && post.scheduledAt ? `Scheduled ${formatDate(post.scheduledAt)}` : `Updated ${formatDate(post.updatedAt)}`}</span></div></div><div className="post-actions"><span className={`status-badge ${post.status.toLowerCase()}`}><span />{titleCase(post.status)}</span><button className="icon-button" title="Edit post" aria-label="Edit post" onClick={() => onEdit(post)}><Pencil size={15} /></button><button className="icon-button danger-button" title="Delete post" aria-label="Delete post" onClick={() => onDelete(post)}><Trash2 size={15} /></button></div></div>
    <p className="post-copy">{post.content}</p>{post.mediaUrl && <img className="post-media" src={post.mediaUrl} alt="Attached post media" loading="lazy" onError={(event) => { event.currentTarget.hidden = true }} />}
    <div className="post-card-bottom"><span><Clock3 size={13} />{post.status === 'SCHEDULED' && post.scheduledAt ? formatDate(post.scheduledAt) : post.status === 'DRAFT' ? 'Not published' : 'Ready to be seen'}</span><button className="post-link" onClick={() => onEdit(post)}>Open post <ArrowRight size={13} /></button></div>
  </article>
}

function initials(name: string) { return name.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase() }
function titleCase(value: string) { return value.toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase()) }
function formatDate(value: string) { return new Date(value).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' }) }
function formatQueueDate(value: string | null) {
  if (!value) return 'NO DATE'
  const date = new Date(value)
  const today = new Date()
  const tomorrow = new Date()
  tomorrow.setDate(today.getDate() + 1)
  if (date.toDateString() === today.toDateString()) return 'TODAY'
  if (date.toDateString() === tomorrow.toDateString()) return 'TOMORROW'
  return date.toLocaleDateString([], { month: 'short', day: 'numeric' }).toUpperCase()
}
function toLocalDateTime(value: string) {
  const date = new Date(value)
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
}

export default App
