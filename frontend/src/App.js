import React from 'react';
import {Button} from "semantic-ui-react";
import "react-responsive-carousel/lib/styles/carousel.min.css";
import './App.css';
import logo from './logo.svg';
import AnimalCards from "./components/animal-cards";
import Carousel from "./components/carousel";
import ChatSidebar from "./components/chat-sidebar";
import {getAnimals, getUsername, sendChatMessage} from "./httpClient";
import {AppContext} from "./AppContext";

const PENDING = 'pending', AUTHENTICATED = 'authenticated', ANONYMOUS = 'anonymous';

let nextMessageId = 1;

export default class App extends React.Component {

    #loginLink = process.env.REACT_APP_LOGIN_URI || '/rescue/login';
    #logoutLink = process.env.REACT_APP_LOGOUT_URI || '/scg-logout?redirect=/rescue';

    constructor(props, context) {
        super(props, context);
        this.state = {
            username: '',
            animals: [],
            userStatus: PENDING,
            chatMessages: [],
            isSidebarOpen: false,
        };
    }

    fetchAnimals() {
        getAnimals().then(animals => this.setState({animals}));
    }

    getUsername = () => {
        getUsername().then(name => this.setState({
            username: name,
            userStatus: name === '' ? ANONYMOUS : AUTHENTICATED,
        }));
    };

    toggleSidebar = () => {
        this.setState(prev => ({isSidebarOpen: !prev.isSidebarOpen}));
    };

    addChatMessage = async (text) => {
        const userMsg = {
            id: nextMessageId++,
            text,
            sender: 'user',
            timestamp: new Date(),
        };
        const assistantMsg = {
            id: nextMessageId++,
            text: '',
            sender: 'assistant',
            timestamp: new Date(),
            isStreaming: true,
        };

        this.setState(prev => ({
            chatMessages: [...prev.chatMessages, userMsg, assistantMsg],
        }));

        const history = this.state.chatMessages
            .filter(m => m.sender !== 'system')
            .map(m => ({role: m.sender, content: m.text}));

        try {
            const response = await sendChatMessage({message: text, history});

            if (response.status === 429) {
                this.setState(prev => {
                    const messages = [...prev.chatMessages];
                    const last = messages[messages.length - 1];
                    messages[messages.length - 1] = {
                        ...last,
                        text: "Whoa there, chatterbox! 🐾 You're talking faster than a parrot on espresso. Give me a moment to catch my breath and try again in a few seconds!",
                        isStreaming: false,
                    };
                    return {chatMessages: messages};
                });
                return;
            }

            if (!response.ok) {
                this.setState(prev => {
                    const messages = [...prev.chatMessages];
                    const last = messages[messages.length - 1];
                    messages[messages.length - 1] = {
                        ...last,
                        text: 'Sorry, something went wrong. Please try again.',
                        isStreaming: false,
                    };
                    return {chatMessages: messages};
                });
                return;
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();

            while (true) {
                const {done, value} = await reader.read();
                if (done) break;

                const chunk = decoder.decode(value);
                // Parse SSE data lines
                const lines = chunk.split('\n');
                for (const line of lines) {
                    if (line.startsWith('data:')) {
                        const raw = line.slice(5);
                        if (raw.trim() === '') continue;
                        let data;
                        try {
                            data = JSON.parse(raw);
                        } catch {
                            data = raw;
                        }
                        this.setState(prev => {
                            const messages = [...prev.chatMessages];
                            const last = messages[messages.length - 1];
                            messages[messages.length - 1] = {
                                ...last,
                                text: last.text + data,
                            };
                            return {chatMessages: messages};
                        });
                    }
                }
            }

            // Mark streaming as complete
            this.setState(prev => {
                const messages = [...prev.chatMessages];
                const last = messages[messages.length - 1];
                messages[messages.length - 1] = {...last, isStreaming: false};
                return {chatMessages: messages};
            });

            // Refresh animal cards if the response indicates a successful adoption
            const lastMsg = this.state.chatMessages[this.state.chatMessages.length - 1];
            if (lastMsg && lastMsg.text.toLowerCase().includes('successfully')) {
                this.fetchAnimals();
            }
        }
        catch (error) {
            console.error('Chat error:', error);
            this.setState(prev => {
                const messages = [...prev.chatMessages];
                const last = messages[messages.length - 1];
                messages[messages.length - 1] = {
                    ...last,
                    text: 'Sorry, I could not connect to the chat server. Please make sure it is running.',
                    isStreaming: false,
                };
                return {chatMessages: messages};
            });
        }
    };

    componentDidMount() {
        this.fetchAnimals();
        this.getUsername();
    }

    render() {
        return (
            <div className={`App ${this.state.isSidebarOpen ? 'sidebar-open' : ''}`}>
                <header className="App-header">
                    <img src={logo} title="Logo" width="250" alt="Logo"/>
                    <div className="header-buttons">
                        {this.getGreetButton()}
                        {this.getActionButton()}
                    </div>
                </header>
                <Carousel/>
                <div className={"App-body"}>
                    <AppContext.Provider value={{refresh: () => this.fetchAnimals()}}>
                        <AnimalCards username={this.state.username}
                                     animals={this.state.animals}/>
                    </AppContext.Provider>
                </div>
                <ChatSidebar
                    isOpen={this.state.isSidebarOpen}
                    messages={this.state.chatMessages}
                    onSendMessage={this.addChatMessage}
                    onClose={this.toggleSidebar}
                    onOpen={this.toggleSidebar}
                    username={this.state.username}
                />
            </div>
        );
    }

    getGreetButton() {
        switch (this.state.userStatus) {
            case ANONYMOUS:
                return <Button disabled color='green' basic> Let meow greet ya! </Button>;
            case AUTHENTICATED:
                return <Button disabled color='green' basic> Have a cute day {this.state.username}! </Button>;
            default:
                return <div/>;
        }
    }
    getActionButton() {
        switch (this.state.userStatus) {
            case ANONYMOUS:
                return (
                    <Button animated='fade' color='green' href={this.#loginLink}>
                        <Button.Content visible>Sign in to adopt</Button.Content>
                        <Button.Content hidden>It only takes a loving heart</Button.Content>
                    </Button>
                );
            case AUTHENTICATED:
                return <Button color='green' href={this.#logoutLink}>Sign out</Button>;
            default:
                return <div/>;
        }
    }
}
