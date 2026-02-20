import { useState, useEffect, useMemo } from "react";
import axios from "axios";
import { AnimatePresence, motion } from "framer-motion";
import { Navbar } from "./components/layout/Navbar";
import { Footer } from "./components/layout/Footer";
import { Hero } from "./sections/Hero";
import { About } from "./sections/About";
import { Notice } from "./sections/Notice";
import { Events } from "./sections/Events";
import { FAQ } from "./sections/FAQ";
import { Board } from "./sections/Board"; // ✨ 추가: 분리된 Board 섹션 임포트

// 모든 페이지 임포트
import { Login } from "./pages/Login";
import { Signup } from "./pages/Signup";
import { FindAccount } from "./pages/FindAccount";
import { SignupSuccess } from "./pages/SignupSuccess";
import { ContactAdmin } from "./pages/ContactAdmin";
import { ProfilePage } from "./pages/ProfilePage";

import { NoticePage } from "./pages/NoticePage";
import { NoticeDetail } from "./pages/NoticeDetail";
import { NoticeWrite } from "./pages/NoticeWrite";

import { EventPage } from "./pages/EventPage";
import { EventDetail } from "./pages/EventDetail";
import { EventWrite } from "./pages/EventWrite";

import { BoardPage } from "./pages/BoardPage";
import { BoardWrite } from "./pages/BoardWrite";
import { BoardDetail } from "./pages/BoardDetail";
import { AssemblyPage } from "./pages/AssemblyPage";
import { AdminPage } from "./pages/AdminPage";
import { MemberDetailTab } from "./pages/tabs/MemberDetailTab";

function App() {
  const [currentPage, setCurrentPage] = useState(() => localStorage.getItem("currentPage") || "home");
  const [isLoggedIn, setIsLoggedIn] = useState(() => localStorage.getItem("isLoggedIn") === "true");
  const [isAdmin, setIsAdmin] = useState(() => localStorage.getItem("isAdmin") === "true");

  const [currentUser, setCurrentUser] = useState<any>(() => {
    const savedUser = localStorage.getItem("currentUser");
    return savedUser ? JSON.parse(savedUser) : null;
  });

  const [userStatus, setUserStatus] = useState("ATTENDING");

  const [selectedMemberLoginId, setSelectedMemberLoginId] = useState<string | null>(() => {
    return localStorage.getItem("selectedMemberLoginId");
  });

  const [selectedNoticeId, setSelectedNoticeId] = useState<number | null>(() => {
    const saved = localStorage.getItem("selectedNoticeId");
    return saved ? Number(saved) : null;
  });
  const [selectedEventId, setSelectedEventId] = useState<number | null>(() => {
    const saved = localStorage.getItem("selectedEventId");
    return saved ? Number(saved) : null;
  });
  const [selectedPostId, setSelectedPostId] = useState<number | null>(() => {
    const saved = localStorage.getItem("selectedPostId");
    return saved ? Number(saved) : null;
  });

  const [posts, setPosts] = useState<any[]>([]);
  const [notices, setNotices] = useState<any[]>([]);
  const [events, setEvents] = useState<any[]>([]);

  useEffect(() => {
    const requestInterceptor = axios.interceptors.request.use((config) => {
      const token = localStorage.getItem("token");
      if (token) {
        config.headers["Authorization"] = `Bearer ${token}`;
      }
      return config;
    });

    const responseInterceptor = axios.interceptors.response.use(
      (response) => {
        if (response.config.url?.includes("/api/members/login")) {
          return response;
        }
        if (response.data && response.data.status === "suspended") {
          alert("관리자에 의해 계정이 정지되었습니다. 즉시 로그아웃됩니다.");
          handleLogout(true);
        }
        return response;
      },
      (error) => {
        if (error.response && (error.response.status === 403 || error.response.data?.status === "suspended")) {
          alert("접근 권한이 없거나 정지된 계정입니다.");
          handleLogout(true);
        }
        return Promise.reject(error);
      }
    );

    return () => {
      axios.interceptors.request.eject(requestInterceptor);
      axios.interceptors.response.eject(responseInterceptor);
    };
  }, []);

  const handleLogout = async (isForced: boolean = false) => {
    if (!isForced && !window.confirm("로그아웃 하시겠습니까?")) {
      return;
    }
    const user = JSON.parse(localStorage.getItem("currentUser") || "{}");
    if (user && user.name) {
      try {
        await axios.post("http://localhost:8080/api/members/logout-log", {
          name: user.name,
          studentId: user.studentId
        });
      } catch (e) {
        console.error("로그아웃 로그 전송 실패", e);
      }
    }
    setIsLoggedIn(false);
    setIsAdmin(false);
    setCurrentUser(null);
    localStorage.clear();
    handleNavigate("home");
    if (isForced) {
      window.location.href = "/";
    }
  };

  const fetchData = async () => {
    try {
      const [postsRes, noticesRes, eventsRes] = await Promise.all([
        axios.get('http://localhost:8080/api/posts'),
        axios.get('http://localhost:8080/api/notices'),
        axios.get('http://localhost:8080/api/events')
      ]);
      if (postsRes.data) setPosts(postsRes.data);
      if (noticesRes.data) setNotices(noticesRes.data);
      if (eventsRes.data) setEvents(eventsRes.data);
    } catch (error) {
      console.error("데이터 로드 에러", error);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  useEffect(() => {
    const handlePopState = (event: PopStateEvent) => {
      if (event.state) {
        const { pageId, itemId } = event.state;
        setCurrentPage(pageId || "home");
        if (pageId === "member-detail") setSelectedMemberLoginId(itemId || null);
        else if (pageId?.startsWith("notice-")) setSelectedNoticeId(itemId || null);
        else if (pageId?.startsWith("event-")) setSelectedEventId(itemId || null);
        else if (pageId?.startsWith("board-detail") || pageId === "board-write") setSelectedPostId(itemId || null);
      } else {
        setCurrentPage("home");
      }
    };
    window.addEventListener("popstate", handlePopState);
    if (!window.history.state) {
      window.history.replaceState({ pageId: currentPage, itemId: selectedPostId || selectedNoticeId || selectedEventId || selectedMemberLoginId }, "");
    }
    return () => window.removeEventListener("popstate", handlePopState);
  }, [currentPage, selectedPostId, selectedNoticeId, selectedEventId, selectedMemberLoginId]);

  useEffect(() => {
    localStorage.setItem("isLoggedIn", isLoggedIn.toString());
    localStorage.setItem("isAdmin", isAdmin.toString());
    localStorage.setItem("currentPage", currentPage);
    if (currentUser) {
      localStorage.setItem("currentUser", JSON.stringify(currentUser));
    } else {
      localStorage.removeItem("currentUser");
    }
    if (selectedNoticeId !== null) localStorage.setItem("selectedNoticeId", selectedNoticeId.toString());
    else localStorage.removeItem("selectedNoticeId");
    if (selectedEventId !== null) localStorage.setItem("selectedEventId", selectedEventId.toString());
    else localStorage.removeItem("selectedEventId");
    if (selectedPostId !== null) localStorage.setItem("selectedPostId", selectedPostId.toString());
    else localStorage.removeItem("selectedPostId");
    if (selectedMemberLoginId !== null) localStorage.setItem("selectedMemberLoginId", selectedMemberLoginId);
    else localStorage.removeItem("selectedMemberLoginId");
  }, [isLoggedIn, isAdmin, currentPage, selectedNoticeId, selectedEventId, selectedPostId, selectedMemberLoginId, currentUser]);

  const handleToggleLike = async (postId: number) => {
    if (!isLoggedIn) { alert("로그인이 필요한 서비스입니다."); return; }
    try {
      const response = await axios.post(`http://localhost:8080/api/posts/${postId}/like`);
      setPosts(prev => prev.map(p => p.id === postId ? response.data : p));
    } catch (e) { console.error("좋아요 처리 실패", e); }
  };

  const handleAddComment = async (postId: number, content: string) => {
    if (!isLoggedIn) { alert("로그인이 필요합니다."); return; }
    try {
      const response = await axios.post(`http://localhost:8080/api/posts/${postId}/comments`, {
        content: content
      });
      setPosts(prev => prev.map(p => p.id === postId ? response.data : p));
    } catch (e) { console.error("댓글 등록 실패", e); }
  };

  const handleAddReply = async (postId: number, commentId: number, content: string) => {
    if (!isLoggedIn) { alert("로그인이 필요합니다."); return; }
    try {
      const response = await axios.post(`http://localhost:8080/api/posts/${postId}/comments`, {
        content: content,
        parentId: commentId
      });
      setPosts(prev => prev.map(p => p.id === postId ? response.data : p));
    } catch (e) { console.error("답글 등록 실패", e); }
  };

  const handleToggleCommentLike = async (postId: number, commentId: number) => {
    if (!isLoggedIn) { alert("로그인이 필요합니다."); return; }
    try {
      const response = await axios.post(`http://localhost:8080/api/posts/${postId}/comments/${commentId}/like`);
      setPosts(prev => prev.map(p => p.id === postId ? response.data : p));
    } catch (e) { console.error("댓글 좋아요 실패", e); }
  };

  const handleDeleteComment = async (postId: number, commentId: number) => {
    if (!window.confirm("댓글을 삭제하시겠습니까?")) return;
    try {
      const response = await axios.delete(`http://localhost:8080/api/posts/${postId}/comments/${commentId}`);
      setPosts(prev => prev.map(p => p.id === postId ? response.data : p));
    } catch (e) { console.error("댓글 삭제 실패", e); }
  };

  const handleDeletePost = async (id: number) => {
    if (window.confirm("이 게시물을 정말로 삭제하시겠습니까?")) {
      try {
        await axios.delete(`http://localhost:8080/api/posts/${id}`);
        setPosts(prev => prev.filter(p => p.id !== id));
        alert("게시물이 삭제되었습니다.");
        handleNavigate("board-page");
      } catch (e) {
        console.error("삭제 실패", e);
      }
    }
  };

  const handleDeleteNotice = async (id: number) => {
    if (!isAdmin || !isLoggedIn) return;
    if (window.confirm("이 공지사항을 삭제하시겠습니까?")) {
      try {
        await axios.delete(`http://localhost:8080/api/notices/${id}`);
        setNotices(prev => prev.filter(n => n.id !== id));
        handleNavigate("notice-page");
        alert("공지사항이 성공적으로 삭제되었습니다. ✨");
      } catch (e) {
        console.error("삭제 실패", e);
      }
    }
  };

  const handleDeleteEvent = async (id: number) => {
    if (!isAdmin || !isLoggedIn) return;
    if (window.confirm("이 행사 기록을 삭제하시겠습니까?")) {
      try {
        await axios.delete(`http://localhost:8080/api/events/${id}`);
        setEvents(prev => prev.filter(e => e.id !== id));
        handleNavigate("event-page");
        alert("행사가 삭제되었습니다.");
      } catch (e) {
        console.error("행사 삭제 실패", e);
      }
    }
  };

  const mainSections = ["home", "events", "notice", "board", "about", "faq"];

  const handleNavigate = (pageId: string, itemId?: any) => {
    window.history.pushState({ pageId, itemId }, "");
    if (pageId === "member-detail") setSelectedMemberLoginId(itemId || null);
    else if (pageId.startsWith("notice-")) setSelectedNoticeId(itemId || null);
    else if (pageId.startsWith("event-")) setSelectedEventId(itemId || null);
    else if (pageId.startsWith("board-detail") || pageId === "board-write") setSelectedPostId(itemId || null);

    if (mainSections.includes(pageId)) {
      if (currentPage !== "home" && !mainSections.includes(currentPage)) {
        setCurrentPage("home");
        setTimeout(() => {
          const el = document.getElementById(pageId);
          if (el) el.scrollIntoView({ behavior: "smooth" });
        }, 150);
      } else {
        const el = document.getElementById(pageId);
        if (el) el.scrollIntoView({ behavior: "smooth" });
        setCurrentPage(pageId);
      }
    } else {
      setCurrentPage(pageId);
      window.scrollTo(0, 0);
    }
  };

  const homeDisplayPosts = useMemo(() => {
    const feePost = posts.find(p => p.category === "회비");
    const otherPosts = posts.filter(p => p.category !== "회비").slice(0, 2);
    const result = [];
    if (feePost) result.push(feePost);
    result.push(...otherPosts);
    return result;
  }, [posts]);

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting && (currentPage === "home" || mainSections.includes(currentPage))) {
            setCurrentPage(entry.target.id);
          }
        });
      },
      { rootMargin: "-20% 0px -70% 0px" }
    );
    mainSections.forEach((id) => {
      const el = document.getElementById(id);
      if (el) observer.observe(el);
    });
    return () => observer.disconnect();
  }, [currentPage]);

  const hideLayout = ["login", "signup", "find-account", "signup-success", "contact-admin", "notice-write", "event-write", "board-write"];

  return (
    <div className="min-h-screen bg-white font-sans selection:bg-indigo-100 selection:text-indigo-700">
      {!hideLayout.includes(currentPage) && (
        <Navbar
          onNavigate={handleNavigate}
          currentPage={currentPage}
          isLoggedIn={isLoggedIn}
          onLogout={() => handleLogout(false)}
          userRole={isAdmin ? "ADMIN" : "USER"}
        />
      )}

      <main>
        {(mainSections.includes(currentPage) || currentPage === "home") ? (
          <>
            <div id="home"><Hero isAdmin={isAdmin && isLoggedIn} /></div>
            <div id="events" className="scroll-mt-20"><Events onNavigate={handleNavigate} events={events.slice(0, 3)} isAdmin={isAdmin && isLoggedIn} isLoggedIn={isLoggedIn} /></div>
            <div id="notice" className="scroll-mt-20"><Notice onNavigate={handleNavigate} notices={notices} /></div>

            {/* ✨ 분리된 Board 섹션 사용 */}
            <Board onNavigate={handleNavigate} posts={homeDisplayPosts} />

            <div id="about" className="scroll-mt-20"><About /></div>
            <div id="faq" className="scroll-mt-20"><FAQ /></div>
          </>
        ) : (
          <AnimatePresence mode="wait">
            {currentPage === "login" ? (
              <Login onNavigate={handleNavigate} onLoginSuccess={(userData) => { setIsLoggedIn(true); setIsAdmin(userData.role === "ADMIN"); setCurrentUser(userData); handleNavigate("home"); }} key="login" />
            ) : currentPage === "signup" ? (
              <Signup onNavigate={handleNavigate} key="signup" />
            ) : currentPage === "find-account" ? (
              <FindAccount onNavigate={handleNavigate} key="find-account" />
            ) : currentPage === "signup-success" ? (
              <SignupSuccess onNavigate={handleNavigate} key="signup-success" />
            ) : currentPage === "profile" ? (
              <ProfilePage onNavigate={handleNavigate} user={currentUser} setUser={setCurrentUser} posts={posts} />
            ) : currentPage === "assembly" ? (
              <AssemblyPage isAdmin={isAdmin} userStatus={userStatus} onNavigate={handleNavigate} loginId={currentUser?.loginId} />
            ) : currentPage === "member-detail" ? (
              <MemberDetailTab
                loginId={selectedMemberLoginId!}
                onBack={() => handleNavigate("assembly")}
              />
            ) : currentPage === "admin" ? (
              <AdminPage />
            ) : currentPage === "contact-admin" ? (
              <ContactAdmin onNavigate={handleNavigate} key="contact-admin" />
            ) : currentPage === "board-page" ? (
              <BoardPage onNavigate={handleNavigate} posts={posts} isAdmin={isAdmin && isLoggedIn} user={currentUser} setPosts={setPosts} key="board-page" />
            ) : currentPage === "board-write" ? (
              <BoardWrite onNavigate={handleNavigate} isAdmin={isAdmin && isLoggedIn} user={currentUser} fetchPosts={fetchData} post={posts.find(p => Number(p.id) === Number(selectedPostId))} key="board-write" />
            ) : currentPage === "board-detail" ? (
              <BoardDetail
                onNavigate={handleNavigate}
                post={posts.find(p => Number(p.id) === Number(selectedPostId))}
                isAdmin={isAdmin && isLoggedIn}
                isLoggedIn={isLoggedIn}
                user={currentUser}
                setPost={(updated: any) => setPosts(prev => prev.map(p => p.id === updated.id ? updated : p))}
                onDelete={handleDeletePost}
                onToggleLike={handleToggleLike}
                onAddComment={handleAddComment}
                onDeleteComment={handleDeleteComment}
                onToggleCommentLike={handleToggleCommentLike}
                onAddReply={handleAddReply}
                key="board-detail"
              />
            ) : currentPage === "notice-page" ? (
              <NoticePage onNavigate={handleNavigate} isAdmin={isAdmin && isLoggedIn} isLoggedIn={isLoggedIn} notices={notices} user={currentUser} fetchNotices={fetchData} key="notice-page" />
            ) : currentPage === "notice-detail" ? (
              <NoticeDetail onNavigate={handleNavigate} isAdmin={isAdmin && isLoggedIn} isLoggedIn={isLoggedIn} notice={notices.find(n => n.id === selectedNoticeId)} user={currentUser} setNotice={(updated: any) => setNotices(prev => prev.map(n => n.id === updated.id ? updated : n))} onDelete={handleDeleteNotice} key="notice-detail" />
            ) : currentPage === "notice-write" ? (
              (isAdmin && isLoggedIn) ? (
                <NoticeWrite onNavigate={handleNavigate} notice={notices.find(n => n.id === selectedNoticeId)} user={currentUser} fetchNotices={fetchData} key="notice-write" />
              ) : (
                <div className="pt-40 text-center h-screen bg-slate-50">{useEffect(() => { alert("간부진만 접근 가능한 페이지입니다."); handleNavigate("home"); }, [])}</div>
              )
            ) : currentPage === "event-page" ? (
              <EventPage onNavigate={handleNavigate} isAdmin={isAdmin && isLoggedIn} isLoggedIn={isLoggedIn} events={events} user={currentUser} setEvents={setEvents} key="event-page" />
            ) : currentPage === "event-detail" ? (
              <EventDetail onNavigate={handleNavigate} isAdmin={isAdmin && isLoggedIn} isLoggedIn={isLoggedIn} event={events.find(e => e.id === selectedEventId)} onDelete={handleDeleteEvent} user={currentUser} setEvent={(updatedEvent: any) => { setEvents(prev => prev.map(e => e.id === updatedEvent.id ? updatedEvent : e)); }} key="event-detail" />
            ) : currentPage === "event-write" ? (
              (isAdmin && isLoggedIn) ? (
                <EventWrite onNavigate={handleNavigate} event={events.find(e => e.id === selectedEventId)} user={currentUser} fetchEvents={fetchData} key="event-write" />
              ) : (
                <div className="pt-40 text-center h-screen bg-slate-50">{useEffect(() => { alert("간부진만 접근 가능한 페이지입니다."); handleNavigate("home"); }, [])}</div>
              )
            ) : (
              <div className="pt-40 text-center h-screen bg-slate-50" key="fallback">
                <h2 className="text-3xl font-black text-slate-900 mb-8 uppercase">{currentPage} Page</h2>
                <button onClick={() => handleNavigate("home")} className="px-10 py-4 bg-indigo-600 text-white rounded-2xl font-bold">홈으로 돌아가기</button>
              </div>
            )}
          </AnimatePresence>
        )}
      </main>

      {!hideLayout.includes(currentPage) && <Footer onNavigate={handleNavigate} />}
    </div>
  );
}

export default App;