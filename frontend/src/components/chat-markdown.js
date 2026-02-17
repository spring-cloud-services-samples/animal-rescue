import React from 'react';
import ReactMarkdown from 'react-markdown';
import './chat-markdown.css';

export default function ChatMarkdown({content}) {
    return (
        <div className="chat-markdown">
            <ReactMarkdown>{content}</ReactMarkdown>
        </div>
    );
}
