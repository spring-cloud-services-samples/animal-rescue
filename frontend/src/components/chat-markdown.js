import React from 'react';
import ReactMarkdown from 'react-markdown';
import rehypeRaw from 'rehype-raw';
import './chat-markdown.css';

export default function ChatMarkdown({content}) {
    return (
        <div className="chat-markdown">
            <ReactMarkdown rehypePlugins={[rehypeRaw]}>{content}</ReactMarkdown>
        </div>
    );
}
