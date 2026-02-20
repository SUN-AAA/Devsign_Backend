import { Hero } from "./components/Hero";
import { Events } from "./components/Events";
import { Notice } from "./components/Notice";
import { Board } from "./components/Board";
import { About } from "./components/About";
import { FAQ } from "./components/FAQ";
import { useMemo } from "react";

interface HomeProps {
    isAdmin: boolean;
    isLoggedIn: boolean;
    events: any[];
    notices: any[];
    posts: any[];
}

export function Home({ isAdmin, isLoggedIn, events, notices, posts }: HomeProps) {
    const homeDisplayPosts = useMemo(() => {
        const feePost = posts.find((p) => p.category === "회비");
        const otherPosts = posts.filter((p) => p.category !== "회비").slice(0, 2);
        const result = [];
        if (feePost) result.push(feePost);
        result.push(...otherPosts);
        return result;
    }, [posts]);

    // 임시 호환성 목적 라우팅 폰 (App에서 react-router를 적용하면 제거/수정될 예정)
    const handleNavigate = (path: string, id?: any) => {
        // 추후 react-router-dom navigate로 대체
    };

    return (
        <>
            <div id="home">
                <Hero isAdmin={isAdmin} />
            </div>
            <div id="events" className="scroll-mt-20">
                <Events onNavigate={handleNavigate} events={events.slice(0, 3)} />
            </div>
            <div id="notice" className="scroll-mt-20">
                <Notice onNavigate={handleNavigate} notices={notices} />
            </div>
            <Board onNavigate={handleNavigate} posts={homeDisplayPosts} />
            <div id="about" className="scroll-mt-20">
                <About />
            </div>
            <div id="faq" className="scroll-mt-20">
                <FAQ />
            </div>
        </>
    );
}
