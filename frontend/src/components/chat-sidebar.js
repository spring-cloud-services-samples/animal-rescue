import React from 'react';
import {Button, Form, Icon} from 'semantic-ui-react';
import * as PropTypes from 'prop-types';
import './chat-sidebar.css';

export default class ChatSidebar extends React.Component {

    constructor(props, context) {
        super(props, context);
        this.state = {
            inputValue: '',
        };
        this.messagesEndRef = React.createRef();
    }

    componentDidUpdate(prevProps) {
        if (prevProps.messages.length !== this.props.messages.length) {
            this.scrollToBottom();
        }
    }

    scrollToBottom = () => {
        if (this.messagesEndRef.current) {
            this.messagesEndRef.current.scrollIntoView({behavior: 'smooth'});
        }
    };

    handleInputChange = (e, {value}) => {
        this.setState({inputValue: value});
    };

    handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            this.handleSend();
        }
    };

    handleSend = () => {
        const text = this.state.inputValue.trim();
        if (text === '') return;
        this.props.onSendMessage(text);
        this.setState({inputValue: ''});
    };

    renderMessage(msg) {
        const isUser = msg.sender === 'user';
        return (
            <div key={msg.id} className={`chat-bubble ${isUser ? 'chat-bubble-user' : 'chat-bubble-assistant'}`}>
                <div className="chat-bubble-text">{msg.text}</div>
            </div>
        );
    }

    render() {
        const {isOpen, messages, onClose, onOpen} = this.props;

        return (
            <>
                {!isOpen && (
                    <button className="chat-fab" onClick={onOpen} aria-label="Open chat">
                        <Icon name="comment" size="large" />
                    </button>
                )}

                {isOpen && (
                    <div className="chat-sidebar-backdrop" onClick={onClose} />
                )}

                <div className={`chat-sidebar ${isOpen ? 'open' : ''}`}>
                    <div className="chat-sidebar-header">
                        <span className="chat-sidebar-title">Chat with us</span>
                        <Icon name="close" link onClick={onClose} className="chat-sidebar-close" />
                    </div>
                    <div className="chat-sidebar-messages">
                        {messages.length === 0 && (
                            <div className="chat-sidebar-empty">
                                Ask us anything about our animals!
                            </div>
                        )}
                        {messages.map(msg => this.renderMessage(msg))}
                        <div ref={this.messagesEndRef} />
                    </div>
                    <div className="chat-sidebar-input">
                        <Form onSubmit={this.handleSend}>
                            <Form.Input
                                placeholder="Type a message..."
                                value={this.state.inputValue}
                                onChange={this.handleInputChange}
                                onKeyDown={this.handleKeyDown}
                                action={
                                    <Button
                                        type="submit"
                                        icon="send"
                                        color="green"
                                    />
                                }
                            />
                        </Form>
                    </div>
                </div>
            </>
        );
    }
}

ChatSidebar.propTypes = {
    isOpen: PropTypes.bool.isRequired,
    messages: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.number.isRequired,
        text: PropTypes.string.isRequired,
        sender: PropTypes.string.isRequired,
    })).isRequired,
    onSendMessage: PropTypes.func.isRequired,
    onClose: PropTypes.func.isRequired,
    onOpen: PropTypes.func.isRequired,
};
