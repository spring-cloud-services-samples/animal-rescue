import React, {useMemo} from 'react';
import DOMPurify from 'dompurify';
import ReactMarkdown from 'react-markdown';
import rehypeRaw from 'rehype-raw';
import './chat-markdown.css';

const HTML_BLOCK_RE = /(<(?:table|ul|ol|div|blockquote)\b[\s\S]*?<\/(?:table|ul|ol|div|blockquote)>)/gi;

function renderMixed(text) {
    const parts = [];
    let lastIndex = 0;
    let match;

    HTML_BLOCK_RE.lastIndex = 0;
    while ((match = HTML_BLOCK_RE.exec(text)) !== null) {
        const before = text.slice(lastIndex, match.index);
        if (before.trim()) {
            parts.push({type: 'md', value: before});
        }
        parts.push({type: 'html', value: match[1]});
        lastIndex = HTML_BLOCK_RE.lastIndex;
    }

    const after = text.slice(lastIndex);
    if (after.trim()) {
        parts.push({type: 'md', value: after});
    }

    return parts.length > 0 ? parts : [{type: 'md', value: text}];
}

function stripCodeWrapping(text) {
    let cleaned = text.replace(/```\w*\n?([\s\S]*?)```/g, '$1');
    cleaned = cleaned.replace(/`(<[^`]+>)`/g, '$1');
    return cleaned;
}

export default function ChatMarkdown({content}) {
    const parts = useMemo(() => renderMixed(stripCodeWrapping(content)), [content]);

    return (
        <div className="chat-markdown">
            {parts.map((part, i) =>
                part.type === 'html'
                    ? <div key={i} dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(part.value)}} />
                    : <ReactMarkdown key={i} rehypePlugins={[rehypeRaw]}>{part.value}</ReactMarkdown>
            )}
        </div>
    );
}
