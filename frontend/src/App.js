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
            isChatBusy: false,
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

    updateAssistantMessage(targetId, updates) {
        this.setState(prev => {
            const messages = [...prev.chatMessages];
            const idx = messages.findIndex(m => m.id === targetId);
            if (idx === -1) return null;
            messages[idx] = {...messages[idx], ...updates};
            return {chatMessages: messages};
        });
    }

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
        const assistantId = assistantMsg.id;

        this.setState(prev => ({
            chatMessages: [...prev.chatMessages, userMsg, assistantMsg],
            isChatBusy: true,
        }));

        const history = this.state.chatMessages
            .filter(m => m.sender !== 'system')
            .map(m => ({role: m.sender, content: m.text}));

        try {
            const response = await sendChatMessage({message: text, history, username: this.state.username});

            if (response.status === 429) {
                this.updateAssistantMessage(assistantId, {
                    text: "Whoa there, chatterbox! 🐾 You're talking faster than a parrot on espresso. Give me a moment to catch my breath and try again in a few seconds!",
                    isStreaming: false,
                });
                this.setState({isChatBusy: false});
                return;
            }

            if (!response.ok) {
                this.updateAssistantMessage(assistantId, {
                    text: 'Sorry, something went wrong. Please try again.',
                    isStreaming: false,
                });
                this.setState({isChatBusy: false});
                return;
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();

            while (true) {
                const {done, value} = await reader.read();
                if (done) break;

                const chunk = decoder.decode(value);
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
                            const idx = messages.findIndex(m => m.id === assistantId);
                            if (idx === -1) return null;
                            messages[idx] = {
                                ...messages[idx],
                                text: messages[idx].text + data,
                            };
                            return {chatMessages: messages};
                        });
                    }
                }
            }

            this.updateAssistantMessage(assistantId, {isStreaming: false});
            this.setState({isChatBusy: false});

            const assistantResult = this.state.chatMessages.find(m => m.id === assistantId);
            if (assistantResult && assistantResult.text.toLowerCase().includes('successfully')) {
                this.fetchAnimals();
            }
        }
        catch (error) {
            console.error('Chat error:', error);
            this.updateAssistantMessage(assistantId, {
                text: 'Sorry, I could not connect to the chat server. Please make sure it is running.',
                isStreaming: false,
            });
            this.setState({isChatBusy: false});
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
                    isBusy={this.state.isChatBusy}
                />
            </div>
        );
    }

    getGreetButton() {
        switch (this.state.userStatus) {
            case ANONYMOUS:
                return <Button disabled style={{color: '#ffffff', backgroundColor: 'transparent', fontSize: '20px', fontWeight: 'bold'}}> Let meow greet ya! </Button>;
            case AUTHENTICATED:
                return <Button disabled style={{color: '#ffffff', backgroundColor: 'transparent', fontSize: '20px', fontWeight: 'bold'}}> Have a cute day {this.state.username}! </Button>;
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
